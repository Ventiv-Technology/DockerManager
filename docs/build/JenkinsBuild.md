# Jenkins Build

- Name: JenkinsBuild
- Properties:
    - jobName: Name of the Jenkins Job to Build
    - server: Server to contact Jenkins
    - authentication: Authentication Type - None, CurrentUser, ProvidedUserPassword
    - user: User Name to call Jenkins - Only useful if authentication = ProvidedUserPassword
    - password: Password to call Jenkins - Only useful if authentication = ProvidedUserPassword
    - buildNumber: Groovy string to evaluate the build number.  Groovy Binding is BuildQueueStatus -> queueStatus, BuildStatus -> status.  Defaults to: ${status.number}
- Outputs:
    - extraParameters.buildStartedResponse: BuildStartedResponse object from the POST to start the Jenkins Job
    - buildingVersion: Build number that has been finished in Jenkins, massaged through Groovy with the buildNumber property.


Kicks off a Jenkins Build via the Jenkins REST Api, and asynchronously waits for it to finish.