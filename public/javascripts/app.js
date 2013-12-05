
$(function() {

    function update(data) {

        if (data.status) {
            var container = $('#status-table tbody');
            container.empty();
            container.prepend($('<tr>').append($('<td>').text(data.status)))
        }
        else if (data.players) {
            $('#players-table tbody').empty();
            $.each(data.players, function() {
                var p = this
                $('#players-table tbody').append(
                    $('<tr>')
                        .append($('<td>').text(p.name))
                        .append($('<td>').text(p.solved + " / " + p.games))
                        .append($('<td>').text(moment(p.lastAction).fromNow()))
                )
            })
        } else if (data.words) {
          var container = $("#wordsContainer");
          if(container) {
            container.empty();
            $.each(data.words, function(idx, word){
              var noBreakCss = "";
              if(word.html.indexOf("_") >= 0){
                noBreakCss = "noBreak ";
              }
              var wordHtml = word.html.replace(/_/g, '<span class="underscore">-</span>');
              
              var node = $('<span class="'+noBreakCss+word.cssClass+'">'+wordHtml+'</span>');
              container.append(node);
            });
          }
        }
        else {
            console.log(data)
        }
    }

    var websocket = new WebSocket(wsUri);
    websocket.onopen = function(evt) {
        update({ status: "Connected"})
    };
    websocket.onclose = function(evt) {
        update({ status: "Disconnected (start server & reload page)"})
    };
    websocket.onmessage = function(evt) { update(jQuery.parseJSON(evt.data)) };
    websocket.onerror = function(evt) { update({ status: evt.data}) };

})