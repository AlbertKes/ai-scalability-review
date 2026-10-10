/* eslint-disable */
const path = require("path");
const fs = require("fs");
const yargs = require("yargs");

// use "npm run build -- --env dev" or "yarn run build --env dev" to check
const env = yargs.argv.env || null;
const profile = yargs.argv.profile || false;

function resolve(relativePath) {
    return path.resolve(__dirname, `../${relativePath}`);
}

module.exports = {
    dist: resolve("build/dist"),
    distStatic: resolve("build/dist/static"),
    src: resolve("src"),
    publicDir: resolve("public"),
    conf: resolve("src/conf"),
    lib: resolve("lib"),
    tsConfig: resolve("tsconfig.json"),
    profile: profile,
    env,
};
