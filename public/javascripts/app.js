
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
                        .append($('<td>').text(""))
                        .append($('<td>').text(""))
                        .append($('<td>').text(p.games))
                        .append($('<td>').text(moment(p.lastAction).fromNow()))
                        .append($('<td>').text(""))
                        .append($('<td>').text(""))
                )
            })
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