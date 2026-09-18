package we.devs.opium.client.modules.client;

import we.devs.opium.api.manager.module.Module;
import we.devs.opium.api.manager.module.RegisterModule;
import we.devs.opium.client.values.impl.ValueCategory;
import we.devs.opium.client.values.impl.ValueEnum;
import we.devs.opium.client.values.impl.ValueNumber;

@RegisterModule(name="Rotations", description="Rotations of the client", category=Module.Category.CLIENT)
public class ModuleRotations extends Module {
    public static ModuleRotations INSTANCE;
    // block settings
    private final ValueCategory blockCategory = new ValueCategory("Block Rotations","Settings for block rotations");
    private final ValueNumber blockSmoothness = new ValueNumber("Smoothness", "Smoothness", "",this.blockCategory, 60, 1, 100);
    private final ValueEnum<Rotations> blockRotationType = new ValueEnum<>("BlockType","BlockType","What type of rotations to use for block rotations.", this.blockCategory, Rotations.Packet);

    // entity settings
    private final ValueCategory entityCategory = new ValueCategory("Entity Rotations","Settings for entity rotations");
    private final ValueNumber entitySmoothness = new ValueNumber("Smoothness", "Smoothness", "",this.entityCategory, 60, 1, 100);
    private final ValueEnum<Rotations> entityRotationType = new ValueEnum<>("EntityType","EntityType","What type of rotations to use for entity rotations.", this.entityCategory, Rotations.NCP);


    public Rotations getBlockRotations() {
        return blockRotationType.getValue();
    }

    public int getBlockSmooth() {
        return (int) blockSmoothness.getValue();
    }

    public Rotations getEntityRotations() {
        return entityRotationType.getValue();
    }

    public int getEntitySmooth() {
        return (int) entitySmoothness.getValue();
    }

    public ModuleRotations() {
        INSTANCE = this;
    }

    public enum Rotations {
        Packet,
        NCP,
        Grim
    }
}
