package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Reconstructor extends UnitBlock{
    public float constructTime = 60 * 2;
    public Seq<UnitType[]> upgrades = new Seq<>();
    public int[] capacities = {};

    public Reconstructor(String name){
        super(name);
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(inRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, inRegion, outRegion, topRegion};
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("progress", 
            (ReconstructorBuild e) -> new Bar(() -> Core.bundle.format("bar.unitprogress", Strings.fixed(e.progress * 100f / constructTime, 0), Strings.fixed((constructTime - e.progress) / (60f * Vars.state.rules.unitBuildSpeedMultiplier * e.timeScale), 0)), 
            () -> Pal.ammo, e::fraction));
        bars.add("units", (ReconstructorBuild e) ->
        new Bar(
            () -> e.unit() == null ? "[lightgray]" + Iconc.cancel :
                Core.bundle.format("bar.unitcap",
                Fonts.getUnicodeStr(e.unit().name),
                e.team.data().countType(e.unit()),
                Units.getCap(e.team)
            ),
            () -> Pal.power,
            () -> e.unit() == null ? 0f : (float)e.team.data().countType(e.unit()) / Units.getCap(e.team)
        ));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.productionTime, constructTime / 60f, StatUnit.seconds);
        stats.add(Stat.output, table -> {
            table.row();
            for(var upgrade : upgrades){
                float size = 8 * 3;
                if(upgrade[0].unlockedNow() && upgrade[1].unlockedNow()){
                    table.image(upgrade[0].icon(Cicon.small)).size(size).padRight(4).padLeft(10).scaling(Scaling.fit).right();
                    table.add(upgrade[0].localizedName).left();

                    table.add("[lightgray] -> ");

                    table.image(upgrade[1].icon(Cicon.small)).size(size).padRight(4).scaling(Scaling.fit);
                    table.add(upgrade[1].localizedName).left();
                    table.row();
                }
            }
        });
    }

    @Override
    public void init(){
        capacities = new int[Vars.content.items().size];
        if(consumes.has(ConsumeType.item) && consumes.get(ConsumeType.item) instanceof ConsumeItems){
            for(ItemStack stack : consumes.<ConsumeItems>get(ConsumeType.item).items){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
                itemCapacity = Math.max(itemCapacity, stack.amount * 2);
            }
        }

        super.init();
    }

    public class ReconstructorBuild extends UnitBuild{

        public float fraction(){
            return progress / constructTime;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return this.payload == null
                && (this.enabled || source == this)
                && relativeTo(source) != rotation
                && payload instanceof UnitPayload pay
                && hasUpgrade(pay.unit.type);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return capacities[item.id];
        }

        @Override
        public void overwrote(Seq<Building> builds){
            if(builds.first().block == block){
                items.add(builds.first().items);
            }
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input
            boolean fallback = true;
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                    fallback = false;
                }
            }
            if(fallback) Draw.rect(inRegion, x, y, rotation * 90);

            Draw.rect(outRegion, x, y, rotdeg());

            if(constructing() && hasArrived()){
                Draw.draw(Layer.blockOver, () -> {
                    Draw.alpha(1f - progress/ constructTime);
                    Draw.rect(payload.unit.type.icon(Cicon.full), x, y, payload.rotation() - 90);
                    Draw.reset();
                    Drawf.construct(this, upgrade(payload.unit.type), payload.rotation() - 90f, progress / constructTime, speedScl, time);
                });
            }else{
                Draw.z(Layer.blockOver);

                drawPayload();
            }

            Draw.z(Layer.blockOver + 0.1f);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void drawBars(){
            super.drawBars();
            Draw.color(Color.black, 0.3f);
            Lines.stroke(4f);
            Lines.line(x - block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f, 
                x + block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f);
            Draw.color(Pal.accent, 1f);
            Lines.stroke(2f);
            Lines.line(x - block.size * tilesize / 2f * 0.6f, y + block.size * tilesize / 2.5f, 
                x + 0.6f * (Mathf.clamp(fraction(), 0f, 1f) - 0.5f) * block.size * tilesize, y + block.size * tilesize / 2.5f);
            Draw.color();
            this.block.drawText((int)(Mathf.clamp(fraction(), 0f, 1f) * 100) + "% | " + Strings.fixed((constructTime - progress) / (60f * Vars.state.rules.unitBuildSpeedMultiplier * timeScale), 0), x, y + block.size * tilesize / 2.5f - 5f, true, 0.9f);
        }

        @Override
        public void updateTile(){
            boolean valid = false;

            if(payload != null){
                //check if offloading
                if(!hasUpgrade(payload.unit.type)){
                    moveOutPayload();
                }else{ //update progress
                    if(moveInPayload()){
                        if(consValid()){
                            valid = true;
                            progress += edelta() * state.rules.unitBuildSpeedMultiplier;
                        }

                        //upgrade the unit
                        if(progress >= constructTime){
                            payload.unit = upgrade(payload.unit.type).create(payload.unit.team());
                            progress %= 1f;
                            Effect.shake(2f, 3f, this);
                            Fx.producesmoke.at(this);
                            consume();
                            Events.fire(new UnitCreateEvent(payload.unit, this));
                        }
                    }
                }
            }

            speedScl = Mathf.lerpDelta(speedScl, Mathf.num(valid), 0.05f);
            time += edelta() * speedScl * state.rules.unitBuildSpeedMultiplier;
        }

        @Override
        public boolean shouldConsume(){
            return constructing();
        }

        public UnitType unit(){
            if(payload == null) return null;

            UnitType t = upgrade(payload.unit.type);
            return t != null && t.unlockedNow() ? t : null;
        }

        public boolean constructing(){
            return payload != null && hasUpgrade(payload.unit.type);
        }

        public boolean hasUpgrade(UnitType type){
            UnitType t = upgrade(type);
            return t != null && t.unlockedNow();
        }

        public UnitType upgrade(UnitType type){
            UnitType[] r =  upgrades.find(u -> u[0] == type);
            return r == null ? null : r[1];
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            if(revision == 1){
                progress = read.f();
            }

        }

    }
}
