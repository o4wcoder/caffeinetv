import { warn, fail, danger } from "danger"
const { _ } = require('lodash');

// Encourage smaller PRs
var bigPRThreshold = 600;
if (danger.github.pr.additions + danger.github.pr.deletions > bigPRThreshold) {
  warn(':exclamation: Big PR');
  markdown('> Pull Request size seems relatively large. If Pull Request contains multiple changes, split each into separate PR will helps faster, easier review.');
}


// Encourage more testing

const modules = [
    {
        directory: 'app',
        sourceDirectory: 'src/main/java',
        testDirectories: ['src/test/java', 'src/androidTest/java']
    }
];

for (let module of modules) {
    const moduleChanges = danger.git.fileMatch(`${module.directory}/${module.sourceDirectory}/**/*.kt`);

    const testChanges = _.reduce(module.testDirectories, (result, value, _) => {
        const testFiles = danger.git.fileMatch(`${module.directory}/${value}/**/*.kt`);
        result.modified = result.modified || testFiles.modified;
        result.created = result.created || testFiles.created;
        return result;
    }, { modified: false, created: false });

    if (moduleChanges.edited && !testChanges.modified && !testChanges.created) {
        if (!danger.github.pr.body.match(new RegExp(`No ${module.sourceDirectory} test changes because`, 'i'))) {
            fail(`No test changes were detected for module ${module.sourceDirectory}.

If there's a reason why you haven't added or changed any tests, please add text to the PR in the format:
"No ${module.sourceDirectory} test changes because *your reason*" and link a JIRA ticket for any follow up work to make this change testable, if applicable.`);
        }
    }
}

