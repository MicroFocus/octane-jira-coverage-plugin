# ALM Octane Quality Insight Plugin for Atlassian Jira

[Official documentation](https://admhelp.microfocus.com/octane/en/all/jira-octane-plugin.htm)

[Microfocus Marketplace]( https://marketplace.microfocus.com/appdelivery/content/alm-octane-test-management-for-jira-plugin)

 
## Overview:
The test coverage plugin enables you to see quality from ALM Octane within JIRA, for JIRA issues that have parallel ALM Octane features and users stories. 
To do this, you need to have a user-defined field (UDF) in the feature or user story in ALM Octane which contains its corresponding JIRA issue key. After you configure the plugin, JIRA shows the ALM Octane test coverage and latest run statuses. For example, this is useful if you manage development in JIRA, and QA in ALM Octane.

The plugin is for JIRA on-premises versions 7.x and 8.x.

Supported ALM Octane versions: CP8, CP9.
 
## Configuration:
The plugin configuration is for one ALM Octane space. You can configure multiple workspaces in this space.

1. Create a UDF of type string in your ALM Octane features and user stories, containing the entity’s JIRA issue key.
You can do this when you synchronize JIRA with ALM Octane, or without synchronization. For example, if you have a user story in ALM Octane to cover the quality of a user story in JIRA, you can create a UDF and manually enter its JIRA issue key.
2. In the plugin configuration screen enter your ALM Octane space details: ALM Octane URL, and client ID and client secret used to access the space. For details, see here.
3. Create one or more workspace configurations for the space as follows:
    * Select a workspace.
    * Enter the ALM Octane UDF in the Mapping field area (for example, jira_key_udf). The icon next to the Mapping field area provides suggestions for relevant UDFs based on the workspace you selected. These are the fields that include jira in their name.
    * The Entity types field is automatically populated with the ALM Octane entities that have this UDF defined, to help you verify that you entered the correct Mapping field.
    * Select one or more JIRA projects and JIRA issue types where you want to see quality from ALM Octane. Each JIRA project can be mapped to one workspace only.

Note: 
* If you experience connection problems, add a proxy in the upper right corner of the screen.
* You can edit a workspace configuration using the ellipses (…) at the right of the workspace configuration table.
 
## ALM Octane Test Coverage Widget Summary:
After setting up the plugin, the ALM Octane Test Coverage widget is added to issue details. The widget shows the number of tests related to the entity, and the summary of the last runs of these tests.

Each test can be executed using more than one configuration, for example using different browsers. The widget shows a summary of the last run status of all the configurations. As a result, an entity might have (for example) 1 test and 3 test runs, since each run reflects a different configuration.
You can drill from the widget to the entity in ALM Octane, and to its related test details. 



## Dev

The plugin is build by using [Atlassian Sdk](https://developer.atlassian.com/server/framework/atlassian-sdk/).

Here are the SDK commands you'll use :

* atlas-run         -- installs this plugin into the product and starts it on localhost
* atlas-debug       -- same as atlas-run, but allows a debugger to attach at port 5005
* atlas-package     -- packages the plugin artifacts 
* atlas-help        -- prints description for all commands in the SDK

//deprecated
* atlas-mvn package -- after atlas-run or atlas-debug, reinstalls the plugin into the running product instance
* atlas-mvn package spotbugs:check --spotbug check



Full documentation is always available at:
https://developer.atlassian.com/display/DOCS/Introduction+to+the+Atlassian+Plugin+SDK
https://developer.atlassian.com/server/framework/atlassian-sdk/atlas-package/
