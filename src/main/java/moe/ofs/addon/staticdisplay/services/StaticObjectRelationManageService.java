package moe.ofs.addon.staticdisplay.services;

public interface StaticObjectRelationManageService {
    void setAssociation(String slot, String runtimeId);

    void removeAssociation(String slot);

    void resetAssociation();
}
