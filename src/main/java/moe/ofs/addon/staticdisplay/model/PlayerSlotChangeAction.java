package moe.ofs.addon.staticdisplay.model;

import lombok.Builder;
import lombok.Data;
import moe.ofs.backend.domain.dcs.poll.PlayerInfo;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class PlayerSlotChangeAction {

    private String previous;

    @Builder.Default
    private String current = StringUtils.EMPTY;

    private PlayerInfo player;
}
