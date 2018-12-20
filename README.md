#ALM Octane Test Coverage Plugin for Atlassian Jira

The plugin supply web panel with test coverage details, that displayed in issue details.
Jira issue must be mapped to Octane entity (Application module, Feature or User story) by defining user-defined field and
filling issue key into this field.

Plugin supply configuration page for configuration of ALM Octane connection details, Octane field, Supported Jira issue types and projects.

The plugin is build by using [Atlassian Sdk](https://developer.atlassian.com/server/framework/atlassian-sdk/).

Here are the SDK commands you'll use :

* atlas-run         -- installs this plugin into the product and starts it on localhost
* atlas-debug       -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-mvn package -- after atlas-run or atlas-debug, reinstalls the plugin into the running product instance
* atlas-package     -- packages the plugin artifacts 
* atlas-help        -- prints description for all commands in the SDK


Full documentation is always available at:
https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
https://developer.atlassian.com/server/framework/atlassian-sdk/atlas-package/
