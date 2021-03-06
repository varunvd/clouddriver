/*
 * Copyright 2018 Lookout, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.ecs.services

import com.google.common.collect.Sets
import com.netflix.spinnaker.clouddriver.aws.cache.Keys
import com.netflix.spinnaker.clouddriver.aws.model.AmazonSubnet
import com.netflix.spinnaker.clouddriver.aws.provider.view.AmazonSubnetProvider
import com.netflix.spinnaker.clouddriver.aws.security.NetflixAmazonCredentials
import com.netflix.spinnaker.clouddriver.ecs.model.EcsSubnet
import com.netflix.spinnaker.clouddriver.ecs.provider.view.AmazonPrimitiveConverter
import com.netflix.spinnaker.clouddriver.ecs.provider.view.EcsAccountMapper
import spock.lang.Specification

class SubnetSelectorSpec extends Specification {

  public static final String ECS_ACCOUNT = 'ecsAccount'
  public static final String AWS_ACCOUNT = 'awsAccount'
  public static final String REGION = 'us-west-2'
  public static final String AVAILABILITY_ZONE_1 = 'us-west-2a'
  public static final String AVAILABILITY_ZONE_2 = 'us-west-2b'
  public static final String GOOD_SUBNET_ID_1 = 'subnet-aa123456'
  public static final String GOOD_SUBNET_ID_2 = 'subnet-bb123123'
  public static final String GOOD_SUBNET_ID_3 = 'subnet-dd345345'
  public static final String GOOD_SUBNET_ID_4 = 'subnet-ee345678'
  public static final String BAD_SUBNET_ID = 'subnet-cc233323'
  public static final String BAD_SUBNET_ID_2 = 'subnet-12345678'
  public static final String VPC_ID_1 = 'vpc-1'
  public static final String VPC_ID_2 = 'vpc-2'

  def amazonSubnetProvider = Mock(AmazonSubnetProvider)
  def amazonPrimitiveConverter = Mock(AmazonPrimitiveConverter)
  def accountMapper = Mock(EcsAccountMapper)
  def awsAccount = Mock(NetflixAmazonCredentials)

  def 'should find the right subnet'() {
    given:
    def desiredSubnetIds = [GOOD_SUBNET_ID_2, GOOD_SUBNET_ID_1]
    def subnetTypeNameWeWantToRetrieve = 'goodSubnetType'
    def subnetTypeNameWeDoNotWantToRetrieve = 'badSubnetType'
    def subnet1 = new AmazonSubnet()
    subnet1.account = AWS_ACCOUNT
    subnet1.region = REGION
    subnet1.availabilityZone = AVAILABILITY_ZONE_1
    subnet1.purpose = subnetTypeNameWeWantToRetrieve
    subnet1.id = GOOD_SUBNET_ID_1


    def subnet2 = new AmazonSubnet()
    subnet2.account = AWS_ACCOUNT
    subnet2.region = REGION
    subnet2.availabilityZone = AVAILABILITY_ZONE_1
    subnet2.purpose = subnetTypeNameWeWantToRetrieve
    subnet2.id = GOOD_SUBNET_ID_2

    def subnet3 = new AmazonSubnet()
    subnet3.account = AWS_ACCOUNT
    subnet3.region = REGION
    subnet3.availabilityZone = AVAILABILITY_ZONE_1
    subnet3.purpose = subnetTypeNameWeDoNotWantToRetrieve
    subnet3.id = BAD_SUBNET_ID

    def subnet4 = new AmazonSubnet()
    subnet4.account = AWS_ACCOUNT
    subnet4.region = REGION
    subnet4.availabilityZone = AVAILABILITY_ZONE_2
    subnet4.purpose = subnetTypeNameWeWantToRetrieve
    subnet3.id = BAD_SUBNET_ID_2

    def ecsSubnet1 = new EcsSubnet()
    ecsSubnet1.account = ECS_ACCOUNT
    ecsSubnet1.region = REGION
    ecsSubnet1.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet1.purpose = subnetTypeNameWeWantToRetrieve
    ecsSubnet1.id = GOOD_SUBNET_ID_1

    def ecsSubnet2 = new EcsSubnet()
    ecsSubnet2.account = ECS_ACCOUNT
    ecsSubnet2.region = REGION
    ecsSubnet2.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet2.purpose = subnetTypeNameWeWantToRetrieve
    ecsSubnet2.id = GOOD_SUBNET_ID_2

    def ecsSubnet3 = new EcsSubnet()
    ecsSubnet3.account = ECS_ACCOUNT
    ecsSubnet3.region = REGION
    ecsSubnet3.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet3.purpose = subnetTypeNameWeDoNotWantToRetrieve
    ecsSubnet3.id = BAD_SUBNET_ID

    def ecsSubnet4 = new EcsSubnet()
    ecsSubnet4.account = ECS_ACCOUNT
    ecsSubnet4.region = REGION
    ecsSubnet4.availabilityZone = AVAILABILITY_ZONE_2
    ecsSubnet4.purpose = subnetTypeNameWeWantToRetrieve
    ecsSubnet4.id = BAD_SUBNET_ID_2

    awsAccount.name >> AWS_ACCOUNT

    accountMapper.fromEcsAccountNameToAws(ECS_ACCOUNT) >> awsAccount

    amazonSubnetProvider.getAllMatchingKeyPattern(
      Keys.getSubnetKey('*', REGION, AWS_ACCOUNT)) >> [subnet1, subnet2, subnet3]

    def aws_sets = Sets.newHashSet(subnet1, subnet2, subnet3);
    amazonPrimitiveConverter.convertToEcsSubnet(aws_sets) >> [ecsSubnet1, ecsSubnet2, ecsSubnet3]

    def subnetSelector = new SubnetSelector(amazonSubnetProvider, amazonPrimitiveConverter, accountMapper)


    when:
    def retrievedSubnetIds = subnetSelector.resolveSubnetsIds(
      ECS_ACCOUNT,
      REGION,
      [AVAILABILITY_ZONE_1],
      subnetTypeNameWeWantToRetrieve
    )
    retrievedSubnetIds.sort()
    desiredSubnetIds.sort()

    then:
    retrievedSubnetIds.containsAll(desiredSubnetIds)
    desiredSubnetIds.containsAll(retrievedSubnetIds)
    !desiredSubnetIds.contains(BAD_SUBNET_ID)
    !desiredSubnetIds.contains(BAD_SUBNET_ID_2)
  }

  def 'should find the right subnets if multiple selected'() {
    given:
    def desiredSubnetIds = [GOOD_SUBNET_ID_1, GOOD_SUBNET_ID_2, GOOD_SUBNET_ID_4]
    def subnetTypeNamesWeWantToRetrieve = Sets.newHashSet('goodSubnetType1', 'goodSubnetType2');

    def subnet1 = new AmazonSubnet()
    subnet1.account = AWS_ACCOUNT
    subnet1.region = REGION
    subnet1.availabilityZone = AVAILABILITY_ZONE_1
    subnet1.purpose = 'goodSubnetType1'
    subnet1.id = GOOD_SUBNET_ID_1

    def subnet2 = new AmazonSubnet()
    subnet2.account = AWS_ACCOUNT
    subnet2.region = REGION
    subnet2.availabilityZone = AVAILABILITY_ZONE_1
    subnet2.purpose = 'goodSubnetType1'
    subnet2.id = GOOD_SUBNET_ID_2

    def subnet3 = new AmazonSubnet()
    subnet3.account = AWS_ACCOUNT
    subnet3.region = REGION
    subnet3.availabilityZone = AVAILABILITY_ZONE_1
    subnet3.purpose = 'badSubnetType'
    subnet3.id = BAD_SUBNET_ID

    def subnet4 = new AmazonSubnet()
    subnet4.account = AWS_ACCOUNT
    subnet4.region = REGION
    subnet4.availabilityZone = AVAILABILITY_ZONE_2
    subnet4.purpose = 'goodSubnetType2'
    subnet3.id = BAD_SUBNET_ID_2

    def subnet5 = new AmazonSubnet()
    subnet5.account = AWS_ACCOUNT
    subnet5.region = REGION
    subnet5.availabilityZone = AVAILABILITY_ZONE_1
    subnet5.purpose = 'goodSubnetType2'
    subnet5.id = GOOD_SUBNET_ID_4

    def ecsSubnet1 = new EcsSubnet()
    ecsSubnet1.account = ECS_ACCOUNT
    ecsSubnet1.region = REGION
    ecsSubnet1.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet1.purpose = 'goodSubnetType1'
    ecsSubnet1.id = GOOD_SUBNET_ID_1

    def ecsSubnet2 = new EcsSubnet()
    ecsSubnet2.account = ECS_ACCOUNT
    ecsSubnet2.region = REGION
    ecsSubnet2.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet2.purpose = 'goodSubnetType1'
    ecsSubnet2.id = GOOD_SUBNET_ID_2

    def ecsSubnet3 = new EcsSubnet()
    ecsSubnet3.account = ECS_ACCOUNT
    ecsSubnet3.region = REGION
    ecsSubnet3.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet3.purpose = 'badSubnetType'
    ecsSubnet3.id = BAD_SUBNET_ID

    def ecsSubnet4 = new EcsSubnet()
    ecsSubnet4.account = ECS_ACCOUNT
    ecsSubnet4.region = REGION
    ecsSubnet4.availabilityZone = AVAILABILITY_ZONE_2
    ecsSubnet4.purpose = 'goodSubnetType2'
    ecsSubnet4.id = BAD_SUBNET_ID_2

    def ecsSubnet5 = new EcsSubnet()
    ecsSubnet5.account = ECS_ACCOUNT
    ecsSubnet5.region = REGION
    ecsSubnet5.availabilityZone = AVAILABILITY_ZONE_1
    ecsSubnet5.purpose = 'goodSubnetType2'
    ecsSubnet5.id = GOOD_SUBNET_ID_4

    awsAccount.name >> AWS_ACCOUNT

    accountMapper.fromEcsAccountNameToAws(ECS_ACCOUNT) >> awsAccount

    amazonSubnetProvider.getAllMatchingKeyPattern(
      Keys.getSubnetKey('*', REGION, AWS_ACCOUNT)) >> [subnet1, subnet2, subnet3, subnet5]

    def aws_sets = Sets.newHashSet(subnet1, subnet2, subnet3, subnet5);
    amazonPrimitiveConverter.convertToEcsSubnet(aws_sets) >> [ecsSubnet1, ecsSubnet2, ecsSubnet3, ecsSubnet5]

    def subnetSelector = new SubnetSelector(amazonSubnetProvider, amazonPrimitiveConverter, accountMapper)


    when:
    def retrievedSubnetIds = subnetSelector.resolveSubnetsIdsForMultipleSubnetTypes(
      ECS_ACCOUNT,
      REGION,
      [AVAILABILITY_ZONE_1],
      subnetTypeNamesWeWantToRetrieve
    )
    retrievedSubnetIds.sort()
    desiredSubnetIds.sort()

    then:
    retrievedSubnetIds.containsAll(desiredSubnetIds)
    desiredSubnetIds.containsAll(retrievedSubnetIds)
    !desiredSubnetIds.contains(BAD_SUBNET_ID)
    !desiredSubnetIds.contains(BAD_SUBNET_ID_2)
  }

  def 'should return the right VPC IDs'() {
    given:
    def desiredVpcIds = [VPC_ID_1, VPC_ID_2]
    def subnetIdsToRetrieve = [GOOD_SUBNET_ID_1, GOOD_SUBNET_ID_2, GOOD_SUBNET_ID_3]

    def subnet1 = new AmazonSubnet()
    subnet1.account = AWS_ACCOUNT
    subnet1.region = REGION
    subnet1.vpcId = VPC_ID_1
    subnet1.id = GOOD_SUBNET_ID_1

    def subnet2 = new AmazonSubnet()
    subnet2.account = AWS_ACCOUNT
    subnet2.region = REGION
    subnet2.vpcId = VPC_ID_2
    subnet2.id = GOOD_SUBNET_ID_2

    def subnet3 = new AmazonSubnet()
    subnet3.account = AWS_ACCOUNT
    subnet3.region = REGION
    subnet3.vpcId = VPC_ID_1
    subnet3.id = GOOD_SUBNET_ID_3

    def ecsSubnet1 = new EcsSubnet()
    ecsSubnet1.account = ECS_ACCOUNT
    ecsSubnet1.region = REGION
    ecsSubnet1.vpcId = VPC_ID_1
    ecsSubnet1.id = GOOD_SUBNET_ID_1

    def ecsSubnet2 = new EcsSubnet()
    ecsSubnet2.account = ECS_ACCOUNT
    ecsSubnet2.region = REGION
    ecsSubnet2.vpcId = VPC_ID_2
    ecsSubnet2.id = GOOD_SUBNET_ID_2

    def ecsSubnet3 = new EcsSubnet()
    ecsSubnet3.account = ECS_ACCOUNT
    ecsSubnet3.region = REGION
    ecsSubnet3.vpcId = VPC_ID_1
    ecsSubnet3.id = GOOD_SUBNET_ID_3

    awsAccount.name >> AWS_ACCOUNT

    accountMapper.fromEcsAccountNameToAws(ECS_ACCOUNT) >> awsAccount

    def subnet_keys = Sets.newHashSet(
      Keys.getSubnetKey(subnet1.id, REGION, AWS_ACCOUNT),
      Keys.getSubnetKey(subnet2.id, REGION, AWS_ACCOUNT),
      Keys.getSubnetKey(subnet3.id, REGION, AWS_ACCOUNT)
    )
    amazonSubnetProvider.loadResults(subnet_keys) >> [subnet1, subnet2, subnet3]

    def aws_sets = Sets.newHashSet(subnet1, subnet2, subnet3);
    amazonPrimitiveConverter.convertToEcsSubnet(aws_sets) >> [ecsSubnet1, ecsSubnet2, ecsSubnet3]

    def subnetSelector = new SubnetSelector(amazonSubnetProvider, amazonPrimitiveConverter, accountMapper)

    when:
    def retrievedVpcIds = subnetSelector.getSubnetVpcIds(
      ECS_ACCOUNT,
      REGION,
      subnetIdsToRetrieve
    )
    retrievedVpcIds.sort()
    desiredVpcIds.sort()

    then:
    retrievedVpcIds.containsAll(desiredVpcIds)
    desiredVpcIds.containsAll(retrievedVpcIds)
  }
}
