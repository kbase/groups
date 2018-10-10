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
        <input id="url"/><button id="seturl">Set</button>
      </div>
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
      this.serviceUrl = $('#url').val();
      console.log("Switched service url to " + this.serviceUrl);
  }
};