package mindustry.world.blocks.distribution;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import mindustry.core.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class ExtendingItemBridge extends ItemBridge{

    public ExtendingItemBridge(String name){
        super(name);
        hasItems = true;
    }

    public class ExtendingItemBridgeBuild extends ItemBridgeBuild{
        @Override
        public void draw(){
            Draw.rect(region, x, y);

            Draw.z(Layer.power);

            //draw each item this bridge have
            if(items != null && Core.settings.getBool("blockBridgeItem")){
                Draw.color(Color.white, 0.8f);
                int loti = 0;
                for(int iid = 0; iid < items.length(); iid++){
                    if(items.get(iid) > 0){
                        for(int itemid = 1; itemid <= items.get(iid); itemid++){
                            Draw.rect(
                            content.item(iid).icon(Cicon.medium), 
                            x, 
                            y - tilesize/2f + 1f + 0.6f * (float)loti,
                            4f,
                            4f
                            );
                            loti++;
                        }
                    }
                }
            }

            Tile other = world.tile(link);
            if(!linkValid(tile, other)) return;

            int i = tile.absoluteRelativeTo(other.x, other.y);

            float ex = other.worldx() - x - Geometry.d4(i).x * tilesize / 2f,
                ey = other.worldy() - y - Geometry.d4(i).y * tilesize / 2f;

            float uptime = state.isEditor() ? 1f : this.uptime;

            ex *= uptime;
            ey *= uptime;

            if(Mathf.zero(Renderer.bridgeOpacity)) return;
            Draw.alpha(Renderer.bridgeOpacity);

            Lines.stroke(8f);
            Lines.line(bridgeRegion,
            x + Geometry.d4(i).x * tilesize / 2f,
            y + Geometry.d4(i).y * tilesize / 2f,
            x + ex,
            y + ey, false);

            Draw.rect(endRegion, x, y, i * 90 + 90);
            Draw.rect(endRegion,
            x + ex + Geometry.d4(i).x * tilesize / 2f,
            y + ey + Geometry.d4(i).y * tilesize / 2f, i * 90 + 270);

            int dist = Math.max(Math.abs(other.x - tile.x), Math.abs(other.y - tile.y));

            int arrows = (dist) * tilesize / 6 - 1;

            Draw.color();

            for(int a = 0; a < arrows; a++){
                Draw.alpha(Mathf.absin(a / (float)arrows - time / 100f, 0.1f, 1f) * uptime * Renderer.bridgeOpacity);
                Draw.rect(arrowRegion,
                x + Geometry.d4(i).x * (tilesize / 2f + a * 6f + 2) * uptime,
                y + Geometry.d4(i).y * (tilesize / 2f + a * 6f + 2) * uptime,
                    i * 90f);
            }
            Draw.reset();
        }
    }
}
