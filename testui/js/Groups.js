import $ from 'jquery';

export default class {
  
  constructor(rootElement) {
    this.rootElement = rootElement;
    this.serviceUrl = 'http://localhost:20001';
    this.token = null;
  }
  
  render() {
    // detach event listeners
    $('button').off('click');
    
    // define html 
    let html = `
      <h2>This is a test/demo ui for the Groups service. It is not ever intended to be used
        in production. Stop complaining about it.
      </h2>
      <div>Set a token to use with the service:</div>
        <input id="token"/><button id="settoken">Set</button>
        <button id="seetoken">See current value</button>
      </div>
      <div>Set service root url:</div>
        <input id="url"/><button id="seturl">Set</button><span id="servroot"></span>
      </div>
      <div id="error"></div>
    `;

    rootElement.innerHTML = html;
    $('#url').val(this.serviceUrl);
    
    // attach event listeners
    $('#settoken').on('click', () => {
        this.setToken();
    });
    $('#seetoken').on('click', () => {
        this.seeToken();
    })
    $('#seturl').on('click', () => {
        this.setURL();
    });
  }
  
  setToken() {
      this.token = $('#token').val();
      $('#token').val("");
  }
  
  seeToken() {
      alert("Token: " + this.token);
  }
  
  setURL() {
      $('#error').text("");
      $('#servroot').text("");
      var url = $('#url').val();
      fetch(url)
         .then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     var d = new Date(0);
                     d.setUTCMilliseconds(json.servertime);
                     $('#servroot').html(
                             `<strong>Service name</strong>: ${json.servname} ` +
                             `<strong>version</strong>: ${json.version} ` + 
                             `<strong>time</strong>: ${d} ` +
                             `<strong>commit</strong>: ${json.gitcommithash} `
                             );
                     this.serviceUrl = url;
                     console.log("Switched service url to " + this.serviceUrl);
                 }).catch(function(err) {
                     console.log(err);
                     $('#error').text(err);
                 });
             } else {
                 response.text().then(function(text) {
                     $('#error').text(text);
                 });
             }
         }).catch(function(err) {
             $('#error').text(err);
         })
  }
};