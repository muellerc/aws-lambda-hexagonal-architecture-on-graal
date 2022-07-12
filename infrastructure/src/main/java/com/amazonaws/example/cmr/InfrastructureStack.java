package com.amazonaws.example.cmr;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.services.s3.assets.AssetOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.BundlingOutput.ARCHIVED;

public class InfrastructureStack extends Stack {

    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        Table unicornTable = new Table(this, "UnicornTable", TableProps.builder()
                .partitionKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("id").build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build());

        EventBus eventBridge = EventBus.Builder.create(this, "UnicornEventBus")
                .eventBusName("unicorns")
                .build();

        List<String> readAdapterFunctionPackagingInstructions = Arrays.asList(
                "-c",
                "mvn clean install && cp /asset-input/adapter/read-adapter/target/function.zip /asset-output/"
        );

        List<String> writeAdapterFunctionPackagingInstructions = Arrays.asList(
                "-c",
                "mvn clean install && cp /asset-input/adapter/write-adapter/target/function.zip /asset-output/"
        );

        BundlingOptions readAdapterBuilderOptions = BundlingOptions.builder()
                .command(readAdapterFunctionPackagingInstructions)
                .image(DockerImage.fromRegistry("al2-graalvm:maven"))
                .volumes(singletonList(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED)
                .build();

        BundlingOptions writeAdapterBuilderOptions = BundlingOptions.builder()
                .command(writeAdapterFunctionPackagingInstructions)
                .image(DockerImage.fromRegistry("al2-graalvm:maven"))
                .volumes(singletonList(
                        DockerVolume.builder()
                                .hostPath(System.getProperty("user.home") + "/.m2/")
                                .containerPath("/root/.m2/")
                                .build()
                ))
                .user("root")
                .outputType(ARCHIVED)
                .build();

        Map<String, String> environmentParameter = Map.of(
                "TABLE_NAME", unicornTable.getTableName(),
                "EVENT_BUS", eventBridge.getEventBusName()
        );

        Function createUnicornFunction = new Function(this, "CreateUnicornFunction", FunctionProps.builder()
                .functionName("CreateUnicornFunction")
                .description("CreateUnicornFunction")
                .runtime(Runtime.PROVIDED_AL2)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(writeAdapterBuilderOptions)
                        .build()))
                .handler("com.amazonaws.example.cmr.adapter.primary.CreateUnicornFunction::handleRequest")
                .memorySize(512)
                .environment(environmentParameter)
                .timeout(Duration.seconds(5))
                .logRetention(RetentionDays.ONE_WEEK)
                // only needed for the initialization call during the init phase
                .initialPolicy(List.of(
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("dynamodb:describeEndpoints"))
                                        .resources(List.of("*")).build(),
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("event:describeEventBus"))
                                        .resources(List.of("*")).build()
                        )
                )
                .build());

        Function updateUnicornFunction = new Function(this, "UpdateUnicornFunction", FunctionProps.builder()
                .functionName("UpdateUnicornFunction")
                .description("UpdateUnicornFunction")
                .runtime(Runtime.PROVIDED_AL2)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(writeAdapterBuilderOptions)
                        .build()))
                .handler("com.amazonaws.example.cmr.adapter.primary.UpdateUnicornFunction::handleRequest")
                .memorySize(512)
                .environment(environmentParameter)
                .timeout(Duration.seconds(5))
                .logRetention(RetentionDays.ONE_WEEK)
                // only needed for the initialization call during the init phase
                .initialPolicy(List.of(
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("dynamodb:describeEndpoints"))
                                        .resources(List.of("*")).build(),
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("event:describeEventBus"))
                                        .resources(List.of("*")).build()
                        )
                )
                .build());

        Function deleteUnicornFunction = new Function(this, "DeleteUnicornFunction", FunctionProps.builder()
                .functionName("DeleteUnicornFunction")
                .description("DeleteUnicornFunction")
                .handler("com.amazonaws.example.cmr.adapter.primary.DeleteUnicornFunction::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(writeAdapterBuilderOptions)
                        .build()))
                .memorySize(512)
                .environment(environmentParameter)
                .timeout(Duration.seconds(5))
                .logRetention(RetentionDays.ONE_WEEK)
                // only needed for the initialization call during the init phase
                .initialPolicy(List.of(
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("dynamodb:describeEndpoints"))
                                        .resources(List.of("*")).build(),
                                PolicyStatement.Builder.create()
                                        .effect(Effect.ALLOW)
                                        .actions(List.of("event:describeEventBus"))
                                        .resources(List.of("*")).build()
                        )
                )
                .build());

        Function getUnicornFunction = new Function(this, "GetUnicornFunction", FunctionProps.builder()
                .functionName("GetUnicornFunction")
                .description("GetUnicornFunction")
                .handler("com.amazonaws.example.cmr.adapter.primary.GetUnicornFunction::handleRequest")
                .runtime(Runtime.PROVIDED_AL2)
                .architecture(Architecture.X86_64)
                .code(Code.fromAsset("../software/", AssetOptions.builder()
                        .bundling(readAdapterBuilderOptions)
                        .build()))
                .memorySize(512)
                .environment(environmentParameter)
                .timeout(Duration.seconds(5))
                .logRetention(RetentionDays.ONE_WEEK)
                // only needed for the initialization call during the init phase
                .initialPolicy(List.of(
                        PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of("dynamodb:describeEndpoints"))
                                .resources(List.of("*")).build(),
                        PolicyStatement.Builder.create()
                                .effect(Effect.ALLOW)
                                .actions(List.of("event:describeEventBus"))
                                .resources(List.of("*")).build()
                        )
                )
                .build());

        unicornTable.grantWriteData(createUnicornFunction);
        unicornTable.grantWriteData(updateUnicornFunction);
        unicornTable.grantWriteData(deleteUnicornFunction);
        unicornTable.grantReadData(getUnicornFunction);

        eventBridge.grantPutEventsTo(createUnicornFunction);
        eventBridge.grantPutEventsTo(updateUnicornFunction);
        eventBridge.grantPutEventsTo(deleteUnicornFunction);

        LambdaRestApi restApi = LambdaRestApi.Builder.create(this, "UnicornStore")
                .restApiName("UnicornStore")
                .endpointTypes(List.of(EndpointType.REGIONAL))
                .handler(createUnicornFunction)
                .proxy(false)
                .build();

        software.amazon.awscdk.services.apigateway.Resource unicornResource = restApi.getRoot().addResource("unicorns");
        unicornResource.addMethod("POST", new LambdaIntegration(createUnicornFunction));

        software.amazon.awscdk.services.apigateway.Resource unicornResourceById = unicornResource.addResource("{id}");
        unicornResourceById.addMethod("GET", new LambdaIntegration(getUnicornFunction));
        unicornResourceById.addMethod("PUT", new LambdaIntegration(updateUnicornFunction));
        unicornResourceById.addMethod("DELETE", new LambdaIntegration(deleteUnicornFunction));

        new CfnOutput(this, "api-endpoint", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }
}
