function(store, id)
    local player = net.get_player_info(id)
    local current_slot = player.slot
    return {
        player_net_id = id,
        target_slot = current_slot
    }
end
