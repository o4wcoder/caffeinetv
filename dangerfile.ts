import { warn, message, danger } from "danger"
const { includes } = require('lodash');
const fs = require('fs');

// Encourage smaller PRs
var bigPRThreshold = 600;
if (danger.github.pr.additions + danger.github.pr.deletions > bigPRThreshold) {
  warn(':exclamation: Big PR');
  markdown('> Pull Request size seems relatively large. If Pull Request contains multiple changes, split each into separate PR will helps faster, easier review.');
}

// Encourage more testing
const kotlinOnly = (file) => includes(file, '.kt')
const filesOnly = (file) => fs.existsSync(file) && fs.lstatSync(file).isFile();

const modified = danger.git.modified_files;
const modifiedAppFiles = modified
  .filter(p => includes(p, 'app/src/'))
  .filter(p => filesOnly(p) && kotlinOnly(p));

const hasAppChanges = modifiedAppFiles.length > 0;

const testChanges = modifiedAppFiles.filter(filepath =>
  filepath.includes('test'),
);
const hasTestChanges = testChanges.length > 0;

// Warn if there are library changes, but not tests
if (hasAppChanges && !hasTestChanges) {
  warn(
    "There are library changes, but not tests. That's OK as long as you're refactoring existing code",
  );
}

