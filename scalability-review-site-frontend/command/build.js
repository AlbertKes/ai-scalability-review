/* eslint-disable */
const chalk = require("chalk");
const childProcess = require("child_process");
const fs = require("fs-extra");
const env = require("./env");

function spawn(command, arguments, errorMessage) {
    const isWindows = process.platform === "win32"; // spawn with {shell: true} can solve .cmd resolving, but prettier doesn't run correctly on mac/linux
    const result = childProcess.spawnSync(isWindows ? command + ".cmd" : command, arguments, {stdio: "inherit"});
    if (result.error) {
        console.error(result.error);
        process.exit(1);
    }
    if (result.status !== 0) {
        console.error(chalk`{red.bold ${errorMessage}}`);
        console.error(`non-zero exit code returned, code=${result.status}, command=${command} ${arguments.join(" ")}`);
        process.exit(1);
    }
}

function checkLint() {
    console.info(chalk`{green.bold [task]} {white.bold Check Lint}`);
    spawn("eslint", ["./src", "--cache"]);
}

function cleanup() {
    console.info(chalk`{green.bold [task]} {white.bold cleanup build/dist}`);
    fs.emptyDirSync(env.dist);
}

function copyPublicDir() {
    console.info(chalk`{green.bold [task]} {white.bold copy public folder to dist}`);
    fs.copySync(env.publicDir, env.distStatic, {dereference: true});
}

function build() {
    if (env.env) {
        checkLint();
    }

    if(env.webpackJSON) {
        console.info(chalk`{white.bold [env]} webpackJSON=${JSON.stringify(env.webpackJSON)}`);
    }
    console.info(chalk`{white.bold [env]} conf=${env.conf}`);

    cleanup();

    console.info(chalk`{green.bold [task]} {white.bold vite build}`);
    try {
        spawn("vite", ["build", "--mode", env.env]);
        copyPublicDir();

        if (env.profile) {
            console.info(chalk`{yellow.bold [note]} Profile generation is not supported in Vite by default`);
        }
    } catch (err) {
        console.error(err);
    }
}

build();
