#!/usr/bin/env bash

set -e -x

# This script is a wrapper around the command that runs the GCE VM snapshot
# creation tool - Daisy - for local development and documentation purposes.
#
# It should be run from the root of the Leonardo repo.
#
# gsutil must have been installed.
#
# application_default_credentials.json needs to be copied to jenkins/gce-custom-images/ which is mounted on Daisy container
# Credentials can be refreshed via 'gcloud auth application-default login' with prjoect set to 'broad-dsde-dev' using
# Broad account

# Set this to "true" if you want to validate the workflow without actually executing it
VALIDATE_WORKFLOW="true"

OUTPUT_SNAPSHOT_NAME_SUFFIX="1"
OUTPUT_SNAPSHOT_NAME=leo-gce-snapshot-$(whoami)-$(date +"%Y-%m-%d")-$OUTPUT_SNAPSHOT_NAME_SUFFIX

# The bucket that Daisy uses as scratch area to store source and log files.
# It must exist or Daisy errors out.
# If it doesn't exist, we create it prior to launching Daisy and
# the Daisy workflow cleans up all but daisy.log at the end.
DAISY_BUCKET_PATH="gs://test-leo-gce-snapshot-daisy-scratch-bucket"

PROJECT="broad-dsde-dev"
REGION="us-central1"
ZONE="${REGION}-a"

# Set this to the tag of the Daisy image you had pulled
DAISY_IMAGE_TAG="latest"

# The source directory should contain `application_default_credentials.json`
# which can be generated via `gcloud auth application-default login` and is saved at
# `~/.config/gcloud/application_default_credentials.json` by default.
SOURCE_DIR="/Users/kyuksel/github/leonardo/jenkins/gce-custom-images"

# Create the Daisy scratch bucket if it doesn't exist. The Daisy workflow will clean it up at the end.
gsutil ls $DAISY_BUCKET_PATH || gsutil mb -b on -p $PROJECT -l $REGION $DAISY_BUCKET_PATH

if [[ "$VALIDATE_WORKFLOW" == "true" ]]; then
  DAISY_CONTAINER="gcr.io/compute-image-tools/daisy:${DAISY_IMAGE_TAG} -validate"
else
  DAISY_CONTAINER="gcr.io/compute-image-tools/daisy:${DAISY_IMAGE_TAG}"
fi

docker run -it --rm -v "$SOURCE_DIR":/gce-custom-images \
  $DAISY_CONTAINER \
  -project $PROJECT \
  -zone $ZONE \
  -gcs_path $DAISY_BUCKET_PATH \
  -default_timeout 60m \
  -oauth /gce-custom-images/application_default_credentials.json \
  -var:base_image projects/cos-cloud/global/images/cos-89-16108-403-22 \
  -var:output_snapshot "$OUTPUT_SNAPSHOT_NAME" \
  -var:gce_custom_images_dir /gce-custom-images \
  -var:installation_script_name prepare_gce_snapshot.sh \
  /gce-custom-images/leo_gce_snapshot.wf.json

# Daisy doesn't clean it up all so we remove the bucket manually
gsutil rm -r $DAISY_BUCKET_PATH
