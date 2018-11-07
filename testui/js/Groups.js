import $ from 'jquery';
import DOMPurify from 'dompurify'

export default class {
    
  constructor(rootElement) {
    this.rootElement = rootElement;
    this.serviceUrl = 'http://localhost:8080/';
    this.authUrl = 'https://ci.kbase.us/services/auth/';
    this.token = null;
    this.user = null;
    this.cookieName = 'kbase_session';
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
          <input type="password" id="token"/>
          <button id="settoken" class="btn btn-primary">Set</button>
          <button id="loadtoken" class="btn btn-primary">Reload from cookie</button>
          <span id="useridentity"></span>
        </div>
        <div class="row"><span>Set linkout to workspace view prefix</space>
          <input id="linkout" />
        </div>
        <div class="row">
          <span>Set auth root url:</span>
          <input id="authurl"/>
          <button id="setauthurl" class="btn btn-primary">Set</button>
          <span>Set service root url:</span>
          <input id="url"/>
          <button id="seturl" class="btn btn-primary">Set</button>
        </div>
        <div class="row" id="servroot"></div>
        <div class="row" id="error"></div>
        <div class="row">
          <button id="listview" class="btn btn-primary">List Groups</button>
          <button id="creategroup" class="btn btn-primary">Create Group</button>
          <button id="createdrequests" class="btn btn-primary">Created Requests</button>
          <button id="targetedrequests" class="btn btn-primary">Incoming Requests</button>
        </div>
        <div id="groups"></div>
      </div>
    `;

    rootElement.innerHTML = html;
    $('#url').val(this.serviceUrl);
    $('#authurl').val(this.authUrl);
    $('#linkout').val('https://ci.kbase.us/#tbdwhatgoeshere');
    
    // attach event listeners
    $('#settoken').on('click', () => {
        this.setToken();
    });
    $('#loadtoken').on('click', () => {
        this.loadTokenFromCookie(true);
    })
    $('#seturl').on('click', () => {
        this.setURL();
    });
    $('#setauthurl').on('click', () => {
        this.setAuthURL();
    });
    $('#listview').on('click', () => {
        this.renderGroups();
    });
    $('#creategroup').on('click', () => {
        this.renderCreateOrUpdateGroup({}, "");
    });
    $('#createdrequests').on('click', () => {
        this.renderCreatedRequests();
    });
    $('#targetedrequests').on('click', () => {
        this.renderTargetedRequests();
    });
    
    this.loadTokenFromCookie(false);
  }
  
  // https://stackoverflow.com/a/25490531/643675
  getCookieValue(name) {
      var b = document.cookie.match('(^|;)\\s*' + name + '\\s*=\\s*([^;]+)');
      return b ? b.pop() : '';
  }
  
  seeToken() {
      alert("Token: " + this.sanitize(this.token));
  }
  
  sanitize(dirtydirtystring) {
      return DOMPurify.sanitize(dirtydirtystring, {SAFE_FOR_JQUERY: true});
  }
  
  handleError(errortext) {
      //TODO JS handle errors better - parse json if possible
      $('#error').text(errortext);
  }
  
  getHeaders() {
      const h = {'accept': 'application/json',
                 'content-type': 'application/json'};
      if (this.token) {
          h['authorization'] = this.token;
      }
      return new Headers(h);
  }
  
  loadTokenFromCookie(reportErr) {
      const token = this.getCookieValue(this.cookieName);
      if (token) {
          this.setTokenValue(token);
      } else if (reportErr) {
          $('#error').text("No " + this.cookieName + " cookie set");
      }
  }
  
  setToken() {
      const token = $('#token').val();
      this.setTokenValue(token);
  }
  
  setTokenValue(token) {
      $('#token').val("");
      $('#error').text("");
      fetch(this.authUrl + 'api/V2/me', {"headers": new Headers({'authorization': token})})
         .then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     this.token = token;
                     this.user = json.user;
                     const s = this.sanitize;
                     $('#useridentity').html(
                             `<strong>I. AM. </strong>${s(json.user)}<strong>!</strong>`);
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
  
  setURL() {
      const url = $('#url').val();
      this.setServiceURL(url, this.completeSetURL);
  }
  
  completeSetURL(url) {
      this.serviceUrl = url;
      const s = this.sanitize;
      $('#url').val(s(this.serviceUrl));
      console.log("Switched service url to " + this.serviceUrl);
      this.renderGroups();
  }
  
  setAuthURL() {
      const url = $('#authurl').val();
      this.setServiceURL(url, this.completeAuthUrl);
  }
  
  completeAuthUrl(url) {
      this.authUrl = url;
      const s = this.sanitize;
      $('#authurl').val(s(this.authUrl));
      console.log("Switched auth url to " + this.authUrl);
  }
  
  setServiceURL(url, callback) {
      $('#servroot').text("");
      $('#error').text("");
      fetch(url, {"headers": this.getHeaders()})
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
                     callback.call(this, url);
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
                            <th>${this.getGravatar(g)}${s(g.id)}</th>
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
      fetch(this.serviceUrl + "group/" + groupid,
            {"headers": this.getHeaders()})
        .then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  // TODO NOW break up this monstrosity
                  const c = new Date(json.createdate).toLocaleString();
                  const m = new Date(json.moddate).toLocaleString();
                  const s = this.sanitize;
                  const members = json.members;
                  const admins = json.admins;
                  const priv = !members.includes(this.user) && !admins.includes(this.user) &&
                      json.owner != this.user;
                  let g =
                      `
                      <table class="table">
                        <tbody>
                          <tr><th>ID</th><td>${this.getGravatar(json)}${s(json.id)}</td></tr>
                          <tr><th>Name</th><td>${s(json.name)}</td></tr>
                          <tr><th>Type</th><td>${s(json.type)}</td></tr>
                          <tr><th>Owner</th><td>${s(json.owner)}</td></tr>
                          <tr><th>Created</th><td>${c}</td></tr>
                          <tr><th>Modified</th><td>${m}</td></tr>
                          <tr><th>Description</th><td>${s(json.description)}</td></tr>
                        </tbody>
                      </table>
                      <div><button id="editgroup" class="btn btn-primary">Edit</button></div>
                      `
                  if (priv) {
                      g += `<div>*** Group membership is private ***</div>`;
                  } else {
                      g += `<div>Members</div><table class="table"><tbody>`;
                      for (const m of members) {
                          g += `<tr><td>${s(m)}</td>
                                  <td>
                                    <button id="remove_${s(m)}" class="btn btn-primary">Remove
                                        </button>
                                  </td>
                                  <td>
                                    <button id="promote_${s(m)}" class="btn btn-primary">Promote
                                        </button>
                                  </td>
                                </tr>`
                      }
                      g += `</tbody></table>`;
                  }
                  g += `<div>Administrators</div><table class="table"><tbody>`;
                  for (const a of admins) {
                      g += `<tr><td>${s(a)}</td>
                              <td>
                                <button id="demote_${s(a)}" class="btn btn-primary">Humiliate
                                    </button>
                              </td>
                            </tr>`
                  }
                  g += `</tbody></table>
                        <div>Workspaces</div>
                        <table class="table">
                        <thead>
                          <tr>
                            <th scope="col">ID</th>
                            <th scope="col">Name</th>
                            <th scope="col">Narrative Name</th>
                            <th scope="col">Public</th>
                            <th scope="col">Administrator</th>
                          </tr>
                        </thead>
                        <tbody>
                       `;
                  for (const ws of json.workspaces) {
                      g += `<tr>
                              <th>${s(ws.wsid)}</th>
                              <td>${s(ws.name)}</td>
                              <td>${s(ws.narrname)}</td>
                              <td>${s(ws.public)}</td>
                              <td>${s(ws.admin)}</td>
                              <td>
                                <button id="removews_${s(ws.wsid)}" class="btn btn-primary">Remove
                                      </button>
                              </td></tr>
                           `
                  }
                  g +=
                      `
                      </tbody></table>
                      <button id="requestgroupmembership" class="btn btn-primary">
                        Request group membership</button>
                      <button id="grouprequests" class="btn btn-primary">
                        Requests for group</button>
                      <div>
                        <input id="addmember"/>
                        <button id="addmemberbtn" class="btn btn-primary">Add member</button>
                      </div>
                      <div>
                        <input id="addws" placeholder="Workspace ID"/>
                        <button id="addwsbtn" class="btn btn-primary">Add workspace</button>
                      </div>
                      `;
                  //TODO CODE inactivate button if group member
                  $('#groups').html(g);
                  $('#editgroup').on('click', () => {
                      this.renderCreateOrUpdateGroup(json, "/update");
                  });
                  $('#requestgroupmembership').on('click', () => {
                      this.requestGroupMembership(groupid);
                  });
                  $('#grouprequests').on('click', () => {
                      this.renderGroupRequests(groupid);
                  });
                  $('#addmemberbtn').on('click', () => {
                      const member = $('#addmember').val();
                      this.addMember(groupid, member);
                  });
                  $('#addwsbtn').on('click', () => {
                      const ws = $('#addws').val();
                      this.addWorkspace(groupid, ws);
                  });
                  if (!priv) {
                      for (const m of members) {
                          $(`#remove_${s(m)}`).on('click', () => {
                              this.removeMember(groupid, m);
                          });
                          $(`#promote_${s(m)}`).on('click', () => {
                              this.promoteMember(groupid, m);
                          });
                      }
                  }
                  for (const a of admins) {
                      $(`#demote_${s(a)}`).on('click', () => {
                          this.demoteAdmin(groupid, a);
                      });
                  }
                  for (const ws of json.workspaces) {
                      $(`#removews_${s(ws.wsid)}`).on('click', () => {
                          // TODO only activate button if ws admin or group admin
                          this.removeWorkspace(groupid, ws.wsid);
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
  
  getGravatar(group) {
      if (!group.custom.gravatarhash) {
          return "";
      }
      const url = "https://www.gravatar.com/avatar/" + group.custom.gravatarhash + "?s=80&r=pg";
      return `<img src="${url}"/>`
  }
  
  addMember(groupid, member) {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      fetch(this.serviceUrl + "group/" + groupid + '/user/' + member,
        {"method": "POST",
         "headers": this.getHeaders()
         }).then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     this.renderRequest(json.id);
                 }).catch ( (err) => {
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
  
  removeMember(groupid, member) {
      this.alterMember(groupid, member, "DELETE", "")
  }
  
  promoteMember(groupid, member) {
      this.alterMember(groupid, member, "PUT", "/admin")
  }
  
  demoteAdmin(groupid, admin) {
      this.alterMember(groupid, admin, "DELETE", "/admin")
  }
  
  alterMember(groupid, member, method, urlsuffix) {
      $('#error').text("");
      fetch(this.serviceUrl + "group/" + groupid + "/user/" + member + urlsuffix,
              {"method": method,
               "headers": this.getHeaders()})
        .then( (response) => {
          if (response.ok) {
              this.renderGroup(groupid);
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
         "headers": this.getHeaders()
         }).then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     this.renderRequest(json.id);
                 }).catch ( (err) => {
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
  
  addWorkspace(groupid, ws) {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      fetch(this.serviceUrl + "group/" + groupid + '/workspace/' + ws,
        {"method": "POST",
         "headers": this.getHeaders()
         }).then( (response) => {
             if (response.ok) {
                 response.json().then( (json) => {
                     if (json.complete) {
                         this.renderGroup(groupid)
                     } else {
                         this.renderRequest(json.id);
                     }
                 }).catch ( (err) => {
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
  
  removeWorkspace(groupid, ws) {
      $('#error').text("");
      fetch(this.serviceUrl + "group/" + groupid + "/workspace/" + ws,
              {"method": "DELETE",
               "headers": this.getHeaders()})
        .then( (response) => {
          if (response.ok) {
              this.renderGroup(groupid);
          } else {
              response.text().then( (err) => {
                  this.handleError(err);
              });
          }
      }).catch( (err) => {
          this.handleError(err);
      });
  }
  
  renderGroupRequests(groupid) {
      this.renderRequests(this.serviceUrl + "group/" + groupid + "/requests")
  }
  
  renderCreatedRequests() {
      this.renderRequests(this.serviceUrl + "request/created");
  }
  
  renderTargetedRequests() {
      this.renderRequests(this.serviceUrl + "request/targeted");
  }
  
  renderRequests(requesturl) {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      fetch(requesturl,
        {"headers": this.getHeaders()})
         .then( (response) => {
              if (response.ok) {
                  response.json().then( (json) => {
                      //TODO NOW gotta be a better way than this
                      let gtable =
                          `
                          <table class="table">
                            <thead>
                              <tr>
                                <th scope="col">Requester</th>
                                <th scope="col">Status</th>
                                <th scope="col">Type</th>
                                <th scope="col">Group ID</th>
                                <th scope="col">Target User</th>
                                <th scope="col">Target Workspace</th>
                              </tr>
                            </thead>
                            <tbody>
                          `;
                      const s = this.sanitize;
                      for (const r of json) {
                          gtable +=
                              `
                              <tr id="${s(r.id)}">
                                <td>${s(r.requester)}</td>
                                <td>${s(r.status)}</td>
                                <td>${s(r.type)}</td>
                                <td>${s(r.groupid)}</td>
                                <td>${s(r.targetuser)}</td>
                                <td>${this.renderViewWSButton(r.targetws)}</td>
                              </tr>
                              `
                      }
                      gtable += `</tbody></table>`;
                      $('#groups').html(gtable);
                      for (const r of json) {
                          $(`#${s(r.id)}`).on('click', () => {
                              this.renderRequest(r.id);
                          });
                          if (r.targetws) {
                              $(`#viewws_${s(r.targetws)}`).on('click', (e) => {
                                  e.stopPropagation();
                                  this.viewWS(r.id, r.targetws); 
                              });
                          }
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
  
  renderViewWSButton(wsid) {
      if (!wsid) {
          return '';
      }
      const s = this.sanitize;
      return `<button id="viewws_${s(wsid)}" class="btn btn-primary">View Workspace</button>`
  }
  
  viewWS(requestid, wsid) {
      $('#error').text("");
      fetch(this.serviceUrl + "request/id/" + requestid + "/getperm",
       {"method": "POST",
        "headers": this.getHeaders()
        }).then( (response) => {
          if (response.ok) {
              window.open($("#linkout").val() + '/' + wsid, '_blank');
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
      $('#error').text("");
      fetch(this.serviceUrl + "request/id/" + requestid,
       {"headers": this.getHeaders()
        }).then( (response) => {
          if (response.ok) {
              response.json().then( (json) => {
                  const c = new Date(json.createdate).toLocaleString();
                  const m = new Date(json.moddate).toLocaleString();
                  const e = new Date(json.expiredate).toLocaleString();
                  const canCancel = json.actions.includes('Cancel');
                  const canDeny = json.actions.includes('Deny');
                  const canAccept = json.actions.includes('Accept');
                  const s = this.sanitize;
                  let g =
                      `
                      <table class="table">
                        <tbody>
                          <tr><th>ID</th><td>${s(json.id)}</td></tr>
                          <tr><th>Group ID</th><td>${s(json.groupid)}</td></tr>
                          <tr><th>Requester</th><td>${s(json.requester)}</td></tr>
                          <tr><th>Target user</th><td>${s(json.targetuser)}</td></tr>
                          <tr><th>Target workspace</th>
                              <td>${this.renderViewWSButton(json.targetws)}</td></tr>
                          <tr><th>Type</th><td>${s(json.type)}</td></tr>
                          <tr><th>Status</th><td>${s(json.status)}</td></tr>
                          <tr><th>Created</th><td>${c}</td></tr>
                          <tr><th>Modified</th><td>${m}</td></tr>
                          <tr><th>Expires</th><td>${e}</td></tr>
                        </tbody>
                      </table>
                      `;
                  if (canCancel) {
                      g +=
                      `
                      <div><button id="cancelrequest" class="btn btn-primary">Cancel</button><div>
                      `
                  }
                  if (canAccept) {
                      g +=
                      `
                      <div><button id="acceptrequest" class="btn btn-primary">Accept</button><div>
                      `
                  }
                  if (canDeny) {
                      g +=
                      `
                      <div class="form-group">
                          <label for="denyreason">Denial reason</label>
                          <input class="form-control" id="denyreason" aria-describedby="denyhelp"
                              placeholder="Reason request denied" required>
                          <small id="denyhelp" class="form-text text-muted">
                              The reason the request was denied.
                          </small>
                      </div>
                      <button id="denyrequest" class="btn btn-primary">Deny</button>
                      `
                  }
                  $('#groups').html(g);
                  if (canCancel) {
                      $('#cancelrequest').on('click', () => {
                          this.cancelRequest(requestid);
                      });
                  }
                  if (canAccept) {
                      $('#acceptrequest').on('click', () => {
                          this.acceptRequest(requestid);
                      });
                  }
                  if (canDeny) {
                      $('#denyrequest').on('click', () => {
                          const denyReason = $('#denyreason').val();
                          this.denyRequest(requestid, denyReason);
                      });
                  }
                  if (json.targetws) {
                      $(`#viewws_${s(json.targetws)}`).on('click', () => {
                          this.viewWS(json.id, json.targetws); 
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
  
  cancelRequest(requestid) {
      this.processRequest("/cancel", requestid);
  }
  
  acceptRequest(requestid) {
      this.processRequest("/accept", requestid);
  }
  
  denyRequest(requestid, denialReason) {
      this.processRequest("/deny", requestid, denialReason);
  }
  
  processRequest(posturl, requestid, denialReason) {
      $('#error').text("");
      fetch(this.serviceUrl + "request/id/" + requestid + posturl,
       {"method": "PUT",
        "headers": this.getHeaders(),
        "body": JSON.stringify({"reason": denialReason})
        }).then( (response) => {
          if (response.ok) {
              this.renderCreatedRequests()
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
  
  getValueTerm(value) {
      return value ? `value="${value}"` : '';
  }
  
  renderCreateOrUpdateGroup(group, urlsuffix) {
      $('#error').text("");
      if (!this.checkToken()) {
          return;
      }
      // TODO handle setting the group type correctly
      // note name is required on create, not on update. Since I'm lazy I'm just making it not required
      if (!group.custom) {
          group.custom = '';
      }
      const input = 
          `
          <div>
            <div class="form-group">
              <label for="groupid">ID</label>
              <input class="form-control" id="groupid" aria-describedby="idhelp"
                placeholder="Enter group ID" ${this.getValueTerm(group.id)} required />
              <small id="idhelp" class="form-text text-muted">
                A unique immutable group ID. Lowercase ASCII letters, numbers and hyphens are
                allowed. The first character must be a letter.
              </small>
            </div>
            <div class="form-group">
              <label for="groupname">Name</label>
              <input class="form-control" id="groupname" aria-describedby="namehelp"
                placeholder="Enter group name" ${this.getValueTerm(group.name)} />
              <small id="namehelp" class="form-text text-muted">
                An arbitrary name for the group.
              </small>
            </div>
            <div class="form-group">
              <label for="grouptype">Type</label>
              <select class="form-control" id="grouptype">
                  <option value="Organization">Organization</option>
                  <option value="Project">Project</option>
                  <option value="Team">Team</option>
              </select>
            </div>
            <div class="form-group">
              <label for="groupdesc">Description</label>
              <input class="form-control" id="groupdesc" aria-describedby="deschelp"
                placeholder="Enter group description (optional)"
                ${this.getValueTerm(group.description)} />
              <small id="deschelp" class="form-text text-muted">
                An arbitrary description of the group.
              </small>
            </div>
            <div class="form-group">
              <label for="groupgravatar">Gravatar hash</label>
              <input class="form-control" id="groupgravatar" aria-describedby="gravhelp"
                placeholder="Enter gravatar hash (optional)"
                ${this.getValueTerm(group.custom.gravatarhash)} />
              <small id="gravhelp" class="form-text text-muted">
                The gravatar hash for a gravatar account.
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
          let grav = $('#groupgravatar').val();
          fetch(this.serviceUrl + "group/" + id + urlsuffix,
                {"method": "PUT",
                 "headers": this.getHeaders(),
                 "body": JSON.stringify({"name": name,
                                         "type": type,
                                         "description": desc,
                                         "custom": {"gravatarhash": grav}})
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