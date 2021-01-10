package moe.ofs.addon.staticdisplay.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import moe.ofs.backend.hookinterceptor.HookProcessEntity;


@EqualsAndHashCode(callSuper = true)
@Data
@ToString(callSuper = true)
public class SlotChangeProcessEntity extends HookProcessEntity {
    @SerializedName("player_net_id")
    private int netId;

    @SerializedName("target_slot")
    private String targetSlot;
}
