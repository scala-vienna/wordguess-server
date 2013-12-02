$(function() {

  function update(data) {
    if (data.status) {
    } else if (data.players) {
    } else if (data.game) {
    } else if (data.words) {
      var container = $("#wordsContainer");
      container.empty();
      $.each(data.words, function(idx, word){
        var wordHtml = word.html.replace(/_/g, '<span class="underscore">_</span>');
        var node = $("<span class="+word.cssClass+">"+wordHtml+"</span>");
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