function(store, id)
    -- find player info
    local player = net.get_player_info(id)
    local current_slot = player.slot
    local target_runtime_id = store:get(current_slot)
    if target_runtime_id then
        net.dostring_in("server", "Object.destroy({id_ = " .. target_runtime_id .. "})")
    end
end
