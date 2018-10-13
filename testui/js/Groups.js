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
      <div class="container">
        <div class="row">
          <h2 class="text-center">
            This is a test/demo ui for the Groups service. It is not ever intended to be used
              in production. Stop complaining about it.
          </h2>
        </div>
        <div class="row">Set a token to use with the service:</div>
        <div class="row">
          <input id="token"/>
          <button id="settoken" class="btn btn-primary">Set</button>
          <button id="seetoken" class="btn btn-primary">See current value</button>
        </div>
        <div class="row">Set service root url:</div>
        <div class="row">
          <input id="url"/>
          <button id="seturl" class="btn btn-primary">Set</button>
        </div>
        <div class="row" id="servroot"></div>
        <div class="row" id="error"></div>
        <div class="row">
          <button id="listview" class="btn btn-primary">List Groups</button>
          <button id="creategroup" class="btn btn-primary">Create Group</button>
          <button id="createdrequests" class="btn btn-primary">Created Requests</button>
        </div>
        <div id="groups"></div>
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
    $('#listview').on('click', () => {
        this.renderGroups();
    });
    $('#creategroup').on('click', () => {
        this.renderCreateGroup();
    });
    $('#createdrequests').on('click', () => {
        this.renderCreatedRequests();
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
                             `
                             <table class="table">
                               <thead>
                                 <tr>
                                   <th scope="col">Service name</th>
                                   <th scope="col">Version</th>
                                   <th scope="col">Server time</th>
                                   <th scope="col">Git commit</th>
                                 </tr>
                               </thead>
                               <tbody>
                                 <tr>
                                   <td>${s(json.servname)}</td>
                                   <td>${s(json.version)}</td>
                                   <td>${s(d)}</td>
                                   <td>${s(json.gitcommithash)}</td>
                                 </tr>
                               </tbody>
                             </table>
                             `);
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
                  let gtable =
                      `
                      <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Name</th>
                            <th scope="col">Type</th>
                            <th scope="col">Owner</th>
                          </tr>
                        </thead>
                        <tbody>
                      `;
                  const s = this.sanitize;
                  for (const g of json) {
                      gtable +=
                          `
                          <tr id="${s(g.id)}">
                            <th>${s(g.id)}</th>
                            <td>${s(g.name)}</td>
                            <td>${s(g.type)}</td>
                            <td>${s(g.owner)}</td>
                          </tr>
                          `
                  }
                  gtable += `</tbody></table>`;
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
                      <table class="table">
                        <tbody>
                          <tr><th>ID</th><td>${s(json.id)}</td></tr>
                          <tr><th>Name</th><td>${s(json.name)}</td></tr>
                          <tr><th>Type</th><td>${s(json.type)}</td></tr>
                          <tr><th>Owner</th><td>${s(json.owner)}</td></tr>
                          <tr><th>Created</th><td>${c}</td></tr>
                          <tr><th>Modified</th><td>${m}</td></tr>
                          <tr><th>Description</th><td>${s(json.description)}</td></tr>
                        </tbody>
                      </table>
                      <button id="requestgroupmembership" class="btn btn-primary">
                        Request group membership</button>
                      `;
                  //TODO CODE inactivate button if group member
                  $('#groups').html(g);
                  $('#requestgroupmembership').on('click', () => {
                      this.requestGroupMembership(groupid);
                  });
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
  
  requestGroupMembership(groupid) {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      fetch(this.serviceUrl + "group/" + groupid + '/requestmembership',
        {"method": "POST",
         "headers": new Headers({"authorization": this.token,
                                 "content-type": "application/json"
                                 })
         }).then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     this.renderRequest(json.id);
                 }).catch ( (err) => {
                     this.handleError(err);
                 });
                 // TODO NOW render request
             } else {
                 response.text().then( (err) => {
                     this.handleError(err);
                 });
             }
         }).catch( (err) => {
             this.handleError(err);
         });
  }
  
  renderCreatedRequests() {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      fetch(this.serviceUrl + "request/created",
        {"headers": new Headers({"authorization": this.token,
                                 "content-type": "application/json"
                                 })
                
         }).then( (response) => {
              if (response.ok) {
                  response.json().then( (json) => {
                      //TODO NOW gotta be a better way than this
                      let gtable =
                          `
                          <table class="table">
                            <thead>
                              <tr>
                                <th scope="col">Status</th>
                                <th scope="col">Type</th>
                                <th scope="col">Group ID</th>
                                <th scope="col">Target</th>
                              </tr>
                            </thead>
                            <tbody>
                          `;
                      const s = this.sanitize;
                      for (const r of json) {
                          gtable +=
                              `
                              <tr id="${s(r.id)}">
                                <td>${s(r.status)}</td>
                                <td>${s(r.type)}</td>
                                <td>${s(r.groupid)}</td>
                                <td>${s(r.targetuser)}</td>
                              </tr>
                              `
                      }
                      gtable += `</tbody></table>`;
                      $('#groups').html(gtable);
                      for (const r of json) {
                          $(`#${s(r.id)}`).on('click', () => {
                              this.renderRequest(r.id);
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
  
  renderRequest(requestid) {
      //TODO NOW handle accept / deny / cancel buttons
      $('#error').text("");
      fetch(this.serviceUrl + "request/id/" + requestid,
       {"headers": new Headers({"authorization": this.token})
        }).then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  const c = new Date(json.createdate).toLocaleString();
                  const m = new Date(json.moddate).toLocaleString();
                  const e = new Date(json.expiredate).toLocaleString();
                  const s = this.sanitize;
                  const g =
                      `
                      <table class="table">
                        <tbody>
                          <tr><th>ID</th><td>${s(json.id)}</td></tr>
                          <tr><th>Group ID</th><td>${s(json.groupid)}</td></tr>
                          <tr><th>Requester</th><td>${s(json.requester)}</td></tr>
                          <tr><th>Target user</th><td>${s(json.targetuser)}</td></tr>
                          <tr><th>Type</th><td>${s(json.type)}</td></tr>
                          <tr><th>Status</th><td>${s(json.status)}</td></tr>
                          <tr><th>Created</th><td>${c}</td></tr>
                          <tr><th>Modified</th><td>${m}</td></tr>
                          <tr><th>Expires</th><td>${e}</td></tr>
                        </tbody>
                      </table>
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
  
  checkToken() {
      if (!this.token) {
          this.handleError("Please set a token. Doofus.");
          return false;
      }
      return true;
  }
  
  renderCreateGroup() {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      const input = 
          `
          <div>
            <div class="form-group">
              <label for="groupid">ID</label>
              <input class="form-control" id="groupid" aria-describedby="idhelp"
                placeholder="Enter group ID" required>
              <small id="idhelp" class="form-text text-muted">
                A unique immutable group ID. Lowercase ASCII letters, numbers and hyphens are
                allowed. The first character must be a letter.
              </small>
            </div>
            <div class="form-group">
              <label for="groupname">Name</label>
              <input class="form-control" id="groupname" aria-describedby="namehelp"
                placeholder="Enter group name" required>
              <small id="namehelp" class="form-text text-muted">
                An arbitrary name for the group.
              </small>
            </div>
            <div class="form-group">
              <label for="grouptype">Type</label>
              <select class="form-control" id="grouptype">
                  <option value="organization">Organization</option>
                  <option value="project">Project</option>
                  <option value="team">Team</option>
              </select>
            </div>
            <div class="form-group">
              <label for="groupdesc">Description</label>
              <input class="form-control" id="groupdesc" aria-describedby="deschelp"
                placeholder="Enter group description (optional)" >
              <small id="deschelp" class="form-text text-muted">
                An arbitrary description of the group.
              </small>
            </div>
            <button id="creategroupinput" class="btn btn-primary">Submit</button>
          </div>
          `;
      $('#groups').html(input);
      $('#creategroupinput').on('click', () => {
          //TODO Do validation
          let id = $('#groupid').val();
          let name = $('#groupname').val();
          let type = $('#grouptype').val();
          let desc = $('#groupdesc').val();
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