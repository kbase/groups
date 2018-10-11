import $ from 'jquery';
import DOMPurify from 'dompurify'

export default class {
    
  constructor(rootElement) {
    this.rootElement = rootElement;
    this.serviceUrl = 'http://localhost:8080/';
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
      <div id="servroot"></div>
      <div id="error"></div>
      <div>
        <button id="listview">List Groups</button><button id="creategroup">Create Group</button>
      </div>
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
    $('#listview').on('click', () => {
        this.renderGroups();
    });
    $('#creategroup').on('click', () => {
        this.renderCreateGroup();
    });
  }
  
  setToken() {
      this.token = $('#token').val();
      $('#token').val("");
  }
  
  seeToken() {
      alert("Token: " + this.sanitize(this.token));
  }
  
  sanitize(dirtydirtystring) {
      return DOMPurify.sanitize(dirtydirtystring, {SAFE_FOR_JQUERY: true});
  }
  
  handleError(errortext) {
      //TODO JS handle errors better - parse json if possible\
      $('#error').text(errortext);
  }
  
  setURL() {
      $('#error').text("");
      $('#servroot').text("");
      let url = $('#url').val();
      fetch(url)
         .then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     const d = new Date(json.servertime).toLocaleString();
                     const s = this.sanitize;
                     $('#servroot').html(
                             `<strong>Service name</strong>: ${s(json.servname)} ` +
                             `<strong>version</strong>: ${s(json.version)} ` + 
                             `<strong>time</strong>: ${d} ` +
                             `<strong>commit</strong>: ${s(json.gitcommithash)} `
                             );
                     if (!url.endsWith('/')) {
                         url = url + '/'
                     }
                     this.serviceUrl = url;
                     $('#url').val(s(this.serviceUrl));
                     console.log("Switched service url to " + this.serviceUrl);
                     this.renderGroups();
                 }).catch( (err) => {
                     this.handleError(err);
                 });
             } else {
                 response.text().then( (err) => {
                     this.handleError(err);
                 });
             }
         }).catch( (err) => {
             this.handleError(err);
         });
  }
  
  renderGroups() {
      $('#error').text("");
      fetch(this.serviceUrl + "group").then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  //TODO NOW gotta be a better way than this
                  let gtable = '<table><tr><th>ID</th><th>Name</th><th>Type</th>' +
                      '<th>Owner</th></tr>';
                  const s = this.sanitize;
                  for (const g of json) {
                      gtable += `<tr id="${s(g.id)}"><td>${s(g.id)}</td><td>${s(g.name)}</td>` + 
                          `<td>${s(g.type)}</td><td>${s(g.owner)}</td></tr>`;
                  }
                  gtable += '</table>';
                  $('#groups').html(gtable);
                  for (const g of json) {
                      $(`#${s(g.id)}`).on('click', () => {
                          this.renderGroup(g.id);
                      });
                  }
              }).catch( (err) => {
                  this.handleError(err);
              });
          } else {
              response.text().then( (err) => {
                  this.handleError(err);
              });
          }
      }).catch( (err) => {
          this.handleError(err);
      });
  }
  
  renderGroup(groupid) {
      $('#error').text("");
      fetch(this.serviceUrl + "group/" + groupid).then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  const c = new Date(json.createdate).toLocaleString();
                  const m = new Date(json.moddate).toLocaleString();
                  const s = this.sanitize;
                  const g =
                      `
                      <div><strong>ID</strong>: ${s(json.id)}</div>
                      <div><strong>Name</strong>: ${s(json.name)}</div>
                      <div><strong>Type</strong>: ${s(json.type)}</div>
                      <div><strong>Owner</strong>: ${s(json.owner)}</div>
                      <div><strong>Created</strong>: ${c}</div>
                      <div><strong>Modified</strong>: ${m}</div>
                      <div><strong>Description</strong>: ${s(json.description)}</div>
                      `;
                  $('#groups').html(g);
              }).catch( (err) => {
                  this.handleError(err);
              });
          } else {
              response.text().then( (err) => {
                  this.handleError(err);
              });
          }
      }).catch( (err) => {
          this.handleError(err);
      });
  }
  
  renderCreateGroup() {
      $('#error').text("");
      if (!this.token) {
          this.handleError("Please set a token. Doofus.");
          return;
      }
      // TODO NOW dropdown for type
      const input = 
          `
          <div><strong>ID</strong>: <input id="groupid"/></div>
          <div><strong>Name</strong>: <input id="groupname"/></div>
          <div><strong>Type</strong>: <input id="grouptype"/></div>
          <div><strong>Description</strong>: <input id="groupdescription"/></div>
          <div><button id="creategroupinput">Create</button></div>
          `;
      $('#groups').html(input);
      $('#creategroupinput').on('click', () => {
          let id = $('#groupid').val();
          let name = $('#groupname').val();
          let type = $('#grouptype').val();
          let desc = $('#groupdescription').val();
          fetch(this.serviceUrl + "group/" + id,
                {"method": "PUT",
                 "headers": new Headers({"authorization": this.token,
                                         "content-type": "application/json"
                                         }),
                 "body": JSON.stringify({"name": name, "type": type, "description": desc})
                 }).then( (response) => {
                     if (response.ok) {
                         this.renderGroup(id);
                     } else {
                         response.text().then( (err) => {
                             this.handleError(err);
                         });
                     }
                 }).catch( (err) => {
                     this.handleError(err);
                 });
      });
  }
};