#!/usr/bin/env node
import * as cdk from '@aws-cdk/core';
import { CdkBambooPipelineStack } from '../lib/cdk-bamboo-pipeline-stack';

const app = new cdk.App();
new CdkBambooPipelineStack(app, 'CdkBambooPipelineStack');
