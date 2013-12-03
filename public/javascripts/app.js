
$(function() {

    function update(data) {

        if (data.status) {

            $('#status-table tbody').prepend($('<tr>').append($('<td>').text(data.status)))
            while ($('#status-table tbody tr').length > 15) {
                $('#status-table tbody tr').last().remove()
            }
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
        update({ status: "Disconnected"})
    };
    websocket.onmessage = function(evt) { update(jQuery.parseJSON(evt.data)) };
    websocket.onerror = function(evt) { update({ status: evt.data}) };

})