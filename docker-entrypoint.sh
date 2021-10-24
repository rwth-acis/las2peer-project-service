#!/usr/bin/env bash

set -e

# print all comands to console if DEBUG is set
if [[ ! -z "${DEBUG}" ]]; then
    set -x
fi

# set some helpful variables
export SERVICE_PROPERTY_FILE='etc/i5.las2peer.services.projectService.ProjectService.properties'
export WEB_CONNECTOR_PROPERTY_FILE='etc/i5.las2peer.connectors.webConnector.WebConnector.properties'
export SERVICE_VERSION=$(awk -F "=" '/service.version/ {print $2}' gradle.properties)
export CORE_VERSION=$(awk -F "=" '/core.version/ {print $2}' gradle.properties)
export SERVICE_NAME=$(awk -F "=" '/service.name/ {print $2}' gradle.properties)
export SERVICE_CLASS=$(awk -F "=" '/service.class/ {print $2}' gradle.properties)
export SERVICE=${SERVICE_NAME}.${SERVICE_CLASS}@${SERVICE_VERSION}

function set_in_service_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${SERVICE_PROPERTY_FILE}
}

# check mandatory variables
[[ -z "${SYSTEMS}" ]] && \
    echo "Mandatory variable SYSTEMS is not set. Add -e SYSTEMS={...} to your arguments." && exit 1

set_in_service_config systems ${SYSTEMS}

# check if a new group agent should be generated for the service
if [[ -z "${NEW_GROUP_AGENT}" ]]; then
  # NEW_GROUP_AGENT is undefined
  # we want to add the service agent to an existing group
  [[ -z "${OLD_SERVICE_AGENT_ID}" ]] && \
    echo "Variable NEW_GROUP_AGENT is not set, but OLD_SERVICE_AGENT_ID is not set too. Cannot start service." && exit 1

  [[ -z "${OLD_SERVICE_AGENT_PW}" ]] && \
    echo "Variable NEW_GROUP_AGENT is not set, but OLD_SERVICE_AGENT_PW is not set too. Cannot start service." && exit 1

  [[ -z "${SERVICE_GROUP_ID}" ]] && \
    echo "Variable NEW_GROUP_AGENT is not set, but SERVICE_GROUP_ID is not set too. Cannot start service. Either set NEW_GROUP_AGENT or set SERVICE_GROUP_ID to the id of the previously used service group." && exit 1

  set_in_service_config oldServiceAgentId ${OLD_SERVICE_AGENT_ID}
  set_in_service_config oldServiceAgentPw ${OLD_SERVICE_AGENT_PW}
  set_in_service_config serviceGroupId ${SERVICE_GROUP_ID}
else
  # NEW_GROUP_AGENT is set
  # Check if there does not exist a group agent yet
  if [[ -f "etc/startup/group.xml" ]]; then
    # group.xml exists
    # do not do anything, we use the existing service and group agents
    echo "There already exists a group.xml file. We use the existing service and group agent."
  else 
    echo "Generating a new service agent..."
    sh bin/start_ServiceAgentGenerator.sh ${SERVICE} ${SERVICE_PASSPHRASE} > "etc/startup/${SERVICE}.xml"
    echo -e "\n${SERVICE}.xml;${SERVICE_PASSPHRASE}" >> "etc/startup/passphrases.txt"
    echo "Generating a new group agent for the service..."
    sh bin/start_GroupAgentGenerator.sh "etc/startup/${SERVICE}.xml" > "etc/startup/group.xml"
    # extract id of group agent from group.xml file
    groupId=`sed -n "s:.*<id>\(.*\)</id>.*:\1:p" "etc/startup/group.xml"`
    echo "Group agent was generated. Group agent identifier is: $groupId"
    echo "Using this group id as the service group id."
    set_in_service_config serviceGroupId $groupId
  fi
fi

# set defaults for optional web connector parameters
[[ -z "${START_HTTP}" ]] && export START_HTTP='TRUE'
[[ -z "${START_HTTPS}" ]] && export START_HTTPS='FALSE'
[[ -z "${SSL_KEYSTORE}" ]] && export SSL_KEYSTORE=''
[[ -z "${SSL_KEY_PASSWORD}" ]] && export SSL_KEY_PASSWORD=''
[[ -z "${CROSS_ORIGIN_RESOURCE_DOMAIN}" ]] && export CROSS_ORIGIN_RESOURCE_DOMAIN='*'
[[ -z "${CROSS_ORIGIN_RESOURCE_MAX_AGE}" ]] && export CROSS_ORIGIN_RESOURCE_MAX_AGE='60'
[[ -z "${ENABLE_CROSS_ORIGIN_RESOURCE_SHARING}" ]] && export ENABLE_CROSS_ORIGIN_RESOURCE_SHARING='TRUE'
[[ -z "${OIDC_PROVIDERS}" ]] && export OIDC_PROVIDERS='https://api.learning-layers.eu/o/oauth2,https://accounts.google.com'

# configure web connector properties

function set_in_web_config {
    sed -i "s?${1}[[:blank:]]*=.*?${1}=${2}?g" ${WEB_CONNECTOR_PROPERTY_FILE}
}
set_in_web_config httpPort ${HTTP_PORT}
set_in_web_config httpsPort ${HTTPS_PORT}
set_in_web_config startHttp ${START_HTTP}
set_in_web_config startHttps ${START_HTTPS}
set_in_web_config sslKeystore ${SSL_KEYSTORE}
set_in_web_config sslKeyPassword ${SSL_KEY_PASSWORD}
set_in_web_config crossOriginResourceDomain "${CROSS_ORIGIN_RESOURCE_DOMAIN}"
set_in_web_config crossOriginResourceMaxAge ${CROSS_ORIGIN_RESOURCE_MAX_AGE}
set_in_web_config enableCrossOriginResourceSharing ${ENABLE_CROSS_ORIGIN_RESOURCE_SHARING}
set_in_web_config oidcProviders ${OIDC_PROVIDERS}

# wait for any bootstrap host to be available
if [[ ! -z "${BOOTSTRAP}" ]]; then
    echo "Waiting for any bootstrap host to become available..."
    for host_port in ${BOOTSTRAP//,/ }; do
        arr_host_port=(${host_port//:/ })
        host=${arr_host_port[0]}
        port=${arr_host_port[1]}
        if { </dev/tcp/${host}/${port}; } 2>/dev/null; then
            echo "${host_port} is available. Continuing..."
            break
        fi
    done
fi

# prevent glob expansion in lib/*
set -f
LAUNCH_COMMAND='java -cp lib/* -jar lib/las2peer-bundle-'"${CORE_VERSION}"'.jar -s service -p '"${LAS2PEER_PORT} ${SERVICE_EXTRA_ARGS}"
if [[ ! -z "${BOOTSTRAP}" ]]; then
    LAUNCH_COMMAND="${LAUNCH_COMMAND} -b ${BOOTSTRAP}"
fi

# start the service within a las2peer node
if [[ -z "${@}" ]]
then
  exec ${LAUNCH_COMMAND} uploadStartupDirectory startService\("'""${SERVICE}""'", "'""${SERVICE_PASSPHRASE}""'"\) startWebConnector
else
  exec ${LAUNCH_COMMAND} ${@}
fi