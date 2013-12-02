$(function() {

  function update(data) {
    if (data.status) {
    } else if (data.players) {
    } else if (data.game) {
    } else if (data.words) {
      var container = $("#wordsContainer");
      container.empty();
      $.each(data.words, function(idx, word){
        var node = $("<span class="+word.cssClass+">"+word.html+"</span>");
        container.append(node);
      });
    } else {
      console.log(data)
    }
  }

  var websocket = new WebSocket(wsUri);
  websocket.onopen = function(evt) {
    update({
      status : "Connected"
    })
    $('#connect-button').prop("disabled", true);
  };
  websocket.onclose = function(evt) {
    update({
      status : "Disconnected"
    })
    $('#connect-button').prop("disabled", false);
  };

  websocket.onmessage = function(evt) {
    update(jQuery.parseJSON(evt.data))
  };
  websocket.onerror = function(evt) {
    update({
      status : evt.data
    })
  };

})