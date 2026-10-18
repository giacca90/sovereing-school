// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

if (!process.env.CHROME_BIN) {
	process.env.CHROME_BIN = '/usr/bin/chromium';
}

module.exports = function (config) {
	config.set({
		basePath: '',
		frameworks: ['jasmine'],
		plugins: [require('karma-jasmine'), require('karma-chrome-launcher'), require('karma-jasmine-html-reporter'), require('karma-coverage')],
		client: {
			jasmine: {
				// Jasmine configuration options
			},
			clearContext: false, // leave Jasmine Spec Runner output visible in browser
		},
		jasmineHtmlReporter: {
			suppressAll: true,
		},
		coverageReporter: {
			dir: require('path').join(__dirname, './coverage'),
			subdir: '.',
			reporters: [{ type: 'html' }, { type: 'text-summary' }, { type: 'lcov' }],
		},
		reporters: ['progress', 'kjhtml'],
		browsers: ['ChromeHeadlessCI'],
		customLaunchers: {
			ChromeHeadlessCI: {
				base: 'ChromeHeadless',
				flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage', '--disable-software-rasterizer', '--headless=new'],
			},
		},
		restartOnFileChange: true,
		singleRun: false,
	});
};
