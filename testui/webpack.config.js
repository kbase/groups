const path = require('path');

module.exports = {
  entry: './js/main.js',
  output: {
    path: path.resolve('./build'),
    filename: 'bundle.js'
  }
};
