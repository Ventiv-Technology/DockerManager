Docker Manager Workflow
=======================

Docker Manager is integrated with Activiti to enable workflow integration with your Environments.  It uses Spring Boot
integration with Activiti (http://activiti.org/userguide/index.html#springSpringBoot) so it will automatically deploy any
BPMN files as workflows.  By default, Docker Manager has been configured to look in config/env-config/processes/ for
any files matching the *.bpmn20.xml name.

By default, any BPMN file will work here, but for best results, consult the Activiti documentation on what you can and cannot
do.  It's important to note that with the Spring Integration, you have full access to all of the services that Docker
Manager provides.  For example, if you want to shut down an application as part of a workflow, you can have the following
task:

    `<serviceTask id="shutDownApplication" name="Shutdown Application" activiti:expression="${environmentController.stopApplication(tierName, environmentId, 'application-id')}"/>`
    
When any of the internal services are called, the Authentication object of the person that kicked off the process will
be used.  This means that in the above example, the initator MUST have authorization to shut down the application.

Enabling Workflows for an Environment
-------------------------------------

Docker Manager takes advantage of Activiti's concept of a 'Category' in order to enable a workflow process for a given environment.
For documentation, please see http://activiti.org/userguide/index.html#deploymentCategory.  In short, since there is no code
to deploy a process, you must take advantage of the 'targetNamespace' field in the 'definitions' element in the BPMN file.
The list that is used here is a comma separated list of tierName.environmentName.  For example:

    `<definitions ... targetNamespace='localhost.boot2docker,development.env1' ... >`

Default Variables
-----------------

When any workflow process is started, a set of default variables will be injected into the process.  The following are injected:

- tierName: Tier Name of the process kicking off
- environmentId: Environment Id of the process kicking off
- initiatorAuthenticationObject: Spring Authentication object of the person that kicked off the process
- applicationKey: Each application in the environment is added.  NOTE: the key here is the applicationId (e.g. activiti)
    - serviceName: Each service will be applied here.  Note, it's only the FIRST instance that gets placed here (e.g. To use in BPMN: ${activiti.activiti})
        - server: Server name this instance is running in (e.g. To use in BPMN: ${activiti.activiti.server})
        - port: List of ports for this service (e.g. To use in BPMN: ${activiti.activiti.ports.http})
        
NOTE: The applicationKey and serviceName will have dashes (-) replaced with underscores (_).  This is because when Activiti
evaluates expressions, it will evaluate a dash as the minus operator.  Obviously, this is not what we want, so you will
want to refer to these things with underscores in the BPMN.  Example, my-future-application may look like this in BPMN:
${my_future_application.complicated_service.server}

Authorization
-------------

Activiti users and authentication is shared with however you've configured Docker Manager Authorization.  Upon login, it will
copy your user and authorizations over to the Activiti database.  This means that to assign a task to a user, they will have
had to log in first, to create their Activiti User.

Docker Manager also adheres to the [Process Initiation Authorization] (http://activiti.org/userguide/index.html#security)
section of the Activiti User Manual.  If there is no authorization defined in your BPMN file, the system will assume
all users are authorized to start it.  NOTE: Being able to check progress is also the same authorization, as Activiti
does not have fine-grained authorizations.

Custom Tasks
------------

Beyond the ability to use any service that Docker Manager has coded, several specific tasks have been created.  The reasoning
here is that this enables us to use `extensionElements` in the BPMN to configure those tasks, instead of attempting to
pass all configuration elements in an `activiti:expression` statement.  Look at the code for the list of fields that may be passed in.

- org.ventiv.docker.manager.process.SendRabbitMessageTask: Sends a Message to a RabbitMQ endpoint.
- org.ventiv.docker.manager.process.RestCallTask: Makes a RESTful call to ALL service instances of a type for an application, and flattens the result into an output variable for later decision making.

An example of using the `extensionElements` tags may look like the following.  For example, to send a Rabbit MQ message
to an exchange:

    <serviceTask id="sendRabbitMessage" name="Send Rabbit Message" activiti:class="org.ventiv.docker.manager.process.SendRabbitMessageTask">
        <extensionElements>
            <activiti:field name="hostName" expression="${applicationId.rabbit.server}"/>
            <activiti:field name="port" expression="${applicationId.rabbit.port.amqp}"/>
            <activiti:field name="exchange" stringValue="SystemBroadcast"/>
            <activiti:field name="message" stringValue='{ "dummyKey": "dummyValue" }'/>
            <activiti:field name="headers" expression='__TypeId__=org.ventiv.docker.manager.process.DummyType'/>
            <activiti:field name="contentType" stringValue='json'/>
        </extensionElements>
    </serviceTask>