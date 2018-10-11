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
      <div id="groups"></div>
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
  
  epochToDate(epoch) {
      const d = new Date(0);
      d.setUTCMilliseconds(epoch);
      return d;
  }
  
  setURL() {
      $('#error').text("");
      $('#servroot').text("");
      let url = $('#url').val();
      fetch(url)
         .then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     const d = this.epochToDate(json.servertime);
                     $('#servroot').html(
                             `<strong>Service name</strong>: ${json.servname} ` +
                             `<strong>version</strong>: ${json.version} ` + 
                             `<strong>time</strong>: ${d} ` +
                             `<strong>commit</strong>: ${json.gitcommithash} `
                             );
                     if (!url.endsWith('/')) {
                         url = url + '/'
                     }
                     this.serviceUrl = url;
                     $('#url').val(this.serviceUrl);
                     console.log("Switched service url to " + this.serviceUrl);
                     this.renderGroups();
                 }).catch( (err) => {
                     console.log(err);
                     $('#error').text(err);
                 });
             } else {
                 response.text().then( (text) => {
                     $('#error').text(text);
                 });
             }
         }).catch(function(err) {
             $('#error').text(err);
         })
  }
  
  renderGroups() {
      fetch(this.serviceUrl + "group").then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  //TODO NOW gotta be a better way than this
                  //TODO NOW how detect & handle clicks on table? need to parameterize onclick
                  let gtable = '<table><tr><th>ID</th><th>Name</th><th>Type</th><th>Owner</th><th>Created</th><th>Modified</th></tr>';
                  for (const g of json) {
                      const c = this.epochToDate(g.createdate);
                      const m = this.epochToDate(g.moddate);
                      gtable += `<tr><td>${g.id}</td><td>${g.name}</td><td>${g.type}</td><td>${g.owner}</td><td>${c}</td><td>${m}</td></tr>`;
                  }
                  gtable += '</table>';
                  $('#groups').html(gtable);
              }).catch( (err) => {
                  $('#error').text(text);
              });
          } else {
              response.text().then(function(text) {
                  $('#error').text(text);
              });
          }
      }).catch( (err) => {
          $('#error').text(err);
      });
  }
};