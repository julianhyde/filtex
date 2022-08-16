// resources/barebones.js
const MappingResult = Java.type('net.hydromatic.filtex.JavaScriptTest.MappingResult');

function map(ctx) {
    return new MappingResult({
        myNameIs: ctx.getName(),
        myValueIs: ctx.getValue()
    });
}
