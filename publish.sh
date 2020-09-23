#!/bin/bash
assets=$(cat manifest.json | jq '.artifacts.CdkBambooPipelineStack.metadata."/CdkBambooPipelineStack" | map(select(.type | contains ("aws:cdk:asset")))')
assetsCount=$(echo $assets | jq '. | length')
echo "[" >> params.properties
for (( c=0; c<assetsCount; c++ ))
do  
   if [[ $c -gt 0 ]]
   then
    echo "," >> params.properties
   fi 
   asset=$(echo $assets | jq --arg ind $c '.[$ind|tonumber]') 
   assetPath=$(echo $asset | jq '.data.path')
   s3BucketParameter=$(echo $asset | jq '.data.s3BucketParameter')
   s3KeyParameter=$(echo $asset | jq '.data.s3KeyParameter')
   artifactHashParameter=$(echo $asset | jq '.data.artifactHashParameter')
   sourceHash=$(echo $asset | jq '.data.sourceHash')
   assetPath=$(sed -e 's/^"//' -e 's/"$//' <<<"$assetPath")
   echo "{\"ParameterKey\": ${s3BucketParameter}, \"ParameterValue\": \"${bamboo.custom.aws.cfn.stack.resources.bamboo-assets-stack.outputs.MyPipelineAssetsDeploymentBucket}\"}," >> params.properties
   echo "{\"ParameterKey\": ${s3KeyParameter}, \"ParameterValue\": \"${assetPath}.zip||\"}," >> params.properties
   echo "{\"ParameterKey\": ${artifactHashParameter}, \"ParameterValue\": ${sourceHash}}," >> params.properties
   cd ${assetPath}
   zip -r "${assetPath}.zip" .
   mv  "${assetPath}.zip" ..
   cd ..
   echo $assetPath
done
echo "]" >> params.properties
cat params.properties
echo "new line"
cat ${bamboo.build.working.directory}/params.properties