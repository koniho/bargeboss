#!/usr/bin/env python3
"""Create a standalone, read-only atlas/anchor inspection page; source PNG unchanged."""
import argparse
import base64
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('atlas_png')
parser.add_argument('anchors_json')
parser.add_argument('output_html')
args = parser.parse_args()
data = json.loads(Path(args.anchors_json).read_text())
assert len(data['frames']) == 32 and data['coordinateSpace'] == 'normalized-source-cell'
png = base64.b64encode(Path(args.atlas_png).read_bytes()).decode('ascii')
html = '''<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width">
<title>Barge Boss vessel anchor review</title><style>
body{background:#102d38;color:#e7dfce;font:16px system-ui;margin:18px}button,select,input{font:inherit}
canvas{max-width:100%;height:auto;border:1px solid #52717a}#points{display:flex;flex-wrap:wrap;gap:5px;max-width:800px}
#points button{font-size:12px;background:#28444e;color:#eee;border:1px solid #567;padding:5px}p{max-width:800px}
</style><h2>Barge Boss vessel anchor review</h2>
<p>Guide-projected attachment points require review against finished artwork. Hidden points remain declared.
The gold cross marks the waterline pivot. Select an anchor to show its stable ID and local coordinates.</p>
<p><button id="prev">Previous</button> <input id="frame" type="range" min="0" max="31" value="0">
<button id="next">Next</button> <span id="caption"></span></p>
<p><select id="category"><option value="all">All anchors</option></select>
<label><input type="checkbox" id="hidden" checked>Show occluded estimates</label></p>
<canvas id="view" width="800" height="600"></canvas><p id="details"></p><div id="points"></div>
<script>
const data=__DATA__,image=new Image(),canvas=document.querySelector('#view'),ctx=canvas.getContext('2d');
const slider=document.querySelector('#frame'),category=document.querySelector('#category'),hidden=document.querySelector('#hidden');
const groups=['lights','smokestacks','tires','contacts','towPoints','mooringPoints','cargoOrigins'];
for(const name of groups){const option=document.createElement('option');option.value=name;option.textContent=name+' ('+(data.frames[0][name]||[]).length+')';category.appendChild(option)}
let selected='';
function draw(){const f=data.frames[+slider.value],size=480*f.uniformScale,ox=400-f.waterlinePivot[0]*size,oy=430-f.waterlinePivot[1]*size;
ctx.fillStyle='#173d49';ctx.fillRect(0,0,800,600);
const col=f.frame%8,row=Math.floor(f.frame/8),x=Math.round(col*image.width/8),y=Math.round(row*image.height/4);
const right=Math.round((col+1)*image.width/8),bottom=Math.round((row+1)*image.height/4);
ctx.drawImage(image,x,y,right-x,bottom-y,ox,oy,size,size);
ctx.strokeStyle='#ffd071';ctx.lineWidth=1.5;ctx.beginPath();ctx.moveTo(390,430);ctx.lineTo(410,430);ctx.moveTo(400,420);ctx.lineTo(400,440);ctx.stroke();
document.querySelector('#caption').textContent='Frame '+f.frame+' / '+f.intendedYawDegrees+'°';
const list=document.querySelector('#points');list.replaceChildren();document.querySelector('#details').textContent='';
for(const group of groups){if(category.value!=='all'&&category.value!==group)continue;
for(const a of f[group]||[]){if(!hidden.checked&&a.visibility==='occluded')continue;
const x=ox+a.position[0]*size,y=oy+a.position[1]*size;
ctx.strokeStyle=a.id===selected?'#fff':a.visibility==='occluded'?'#a0788c':'#66e9cb';ctx.lineWidth=a.id===selected?2:1;
ctx.beginPath();ctx.arc(x,y,Math.max(3,a.radius*size),0,Math.PI*2);ctx.moveTo(x-4,y);ctx.lineTo(x+4,y);ctx.moveTo(x,y-4);ctx.lineTo(x,y+4);ctx.stroke();
if(a.id===selected){ctx.fillStyle='#fff';ctx.font='14px system-ui';ctx.fillText(a.id,x+8,y-8);document.querySelector('#details').textContent=a.id+' — local XYZ '+a.localXYZ.join(', ')+'; '+a.visibility+'; '+a.reviewStatus;}
const button=document.createElement('button');button.textContent=a.id;button.onclick=()=>{selected=a.id;draw()};list.appendChild(button);}}
}
slider.oninput=draw;category.onchange=draw;hidden.onchange=draw;
document.querySelector('#prev').onclick=()=>{slider.value=(+slider.value+31)%32;draw()};
document.querySelector('#next').onclick=()=>{slider.value=(+slider.value+1)%32;draw()};
image.onload=draw;image.src='data:image/png;base64,__PNG__';
</script>'''.replace('__DATA__', json.dumps(data, separators=(',', ':')).replace('<', '\\u003c')).replace('__PNG__', png)
output = Path(args.output_html)
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(html)
print(output)
