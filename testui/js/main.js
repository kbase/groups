import Converter from './Converter';

console.log('foo')

const rootElement = document.getElementById('rootElement');
var conv = new Converter(rootElement, 1.11745);
conv.render();