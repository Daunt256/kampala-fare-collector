const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const file=process.argv[2];let checks=0;
const catalog=JSON.parse(fs.readFileSync(process.argv[3]||require('node:path').join(require('node:path').dirname(file),'../config/catalog.json'),'utf8'));
const context={console,Map,Set,Date,Utilities:{formatDate:(date)=>date.toISOString(),DigestAlgorithm:{SHA_256:'sha256'},computeDigest:(_,s)=>Array.from(crypto.createHash('sha256').update(s).digest())}};
vm.createContext(context);vm.runInContext(fs.readFileSync(file,'utf8'),context,{filename:file});
function test(name,fn){fn();checks++;}
const now=Date.now(),config={routes:[{id:'R01',pickup:'Test pickup',dropoff:'Test destination'}],products:[
 {id:'sb_saver',provider:'SafeBoda',mode:'Boda',label:'SafeBoda Saver'},
 {id:'bolt_boda',provider:'Bolt',mode:'Boda',label:'Bolt Boda'},
 {id:'faras_boda',provider:'Faras',mode:'Boda',label:'Faras Boda'}],max_spread_seconds:60};
const quote={id:'aaaa1111-2222-3333-4444-555555555555',run_id:'bbbb1111-2222-3333-4444-555555555555',route_id:'R01',pickup:config.routes[0].pickup,dropoff:config.routes[0].dropoff,provider:'SafeBoda',mode:'Boda',product_id:'sb_saver',low:3000,high:4000,currency:'UGX',raw:'UGX 3,000-4,000',captured_at:now-10000,reviewed:true,route_verification:'user_confirmed',payment_method:'cash',promotion_status:'none_shown',evidence:'Saver | UGX 3,000-4,000'};
const validate=r=>context.validateCollectorRecord_(r,config,'device1111-2222-3333-4444-555555555555',now);
const published='https://script.google.com/macros/s/test-deployment-123/exec',development='https://script.google.com/a/example.test/macros/s/test-development-123/dev';
test('Connection uses saved published URL instead of editor development URL',()=>assert.equal(context.collectorConnectionUrl_(published,development),published));
test('Connection accepts a reported published URL',()=>assert.equal(context.collectorConnectionUrl_(null,published),published));
test('Connection never presents a development URL for phone pairing',()=>assert(!context.collectorConnectionUrl_(null,development).startsWith('https://')));
test('Untrusted saved endpoint cannot override a published Google URL',()=>assert.equal(context.collectorConnectionUrl_('https://example.test/exec',published),published));
test('Connection rejects query strings and unpublished links',()=>assert(!context.collectorConnectionUrl_(published+'?key=test',development).startsWith('https://')));
test('Server catalog matches the provider and product configuration',()=>{assert.deepEqual(JSON.parse(JSON.stringify(context.defaultCollectorProducts_())),catalog.products);assert.deepEqual(Array.from(vm.runInContext('COLLECTOR.providers',context)),catalog.providers.map(p=>p.name));});
test('Valid range retained',()=>{const row=validate(quote);assert.equal(row[10],3000);assert.equal(row[11],4000);});
for(const [name,patch] of [
 ['wrong provider',{provider:'Faras'}],['wrong category',{product_id:'unknown'}],['route changed',{dropoff:'Another mall'}],['unreviewed',{reviewed:false}],['unverified route',{route_verification:'unverified'}],['inconsistent fare',{low:2500}],['string fare',{low:'3000'}],['future time',{captured_at:now+3600000}],['missing time',{captured_at:null}],['wrong currency',{currency:'KES'}],['reversed range',{raw:'UGX 4,000-3,000'}],['ETA instead of fare',{raw:'3 min'}],['unknown payment',{payment_method:'magic'}]
])test('Reject '+name,()=>assert.throws(()=>validate({...quote,...patch})));
test('Range parser rejects promo pair',()=>assert.equal(context.parseCollectorPrice_('UGX 12,000 10,000'),null));
test('Formula injection escaped',()=>assert.equal(context.collectorCell_('=IMPORTXML("x")'),"'=IMPORTXML(\"x\")"));
test('Authentication rejects wrong token',()=>{assert(context.tokenMatches_('test-secret','test-secret'));assert(!context.tokenMatches_('bad','test-secret'));assert(!context.tokenMatches_(null,'test-secret'));});
const a=validate(quote),b=validate({...quote,id:'cccc1111-2222-3333-4444-555555555555',provider:'Bolt',product_id:'bolt_boda',low:3500,high:3500,raw:'3500',captured_at:now-5000}),c=validate({...quote,id:'dddd1111-2222-3333-4444-555555555555',provider:'Faras',product_id:'faras_boda',low:2500,high:2500,raw:'2500',captured_at:now});
test('Same run combines with measured gap',()=>{const rows=context.comparisonRows_([a,b,c],config);assert.equal(rows.length,1);assert.equal(rows[0][7],10);assert.equal(rows[0][8],3);assert(!rows[0][10].includes('Missing'));});
test('Missing providers remain visible',()=>assert(context.comparisonRows_([a],config)[0][10].includes('Missing categories')));
test('Later single capture does not merge with old run',()=>{const newer=c.slice();newer[2]='new-run-1111-2222-3333';newer[13]=now+1;const row=context.comparisonRows_([a,b,c,newer],config)[0];assert.equal(row[8],1);assert(row[10].includes('Missing categories'));});
test('Time gap flagged',()=>{const later=c.slice();later[13]=now+70000;assert(context.comparisonRows_([a,b,later],config)[0][10].includes('Capture gap'));});
test('Payment mismatch flagged',()=>{const wallet=c.slice();wallet[17]='wallet';assert(context.comparisonRows_([a,b,wallet],config)[0][10].includes('Different payment methods'));});
test('Blank prices remain blank',()=>{const row=context.comparisonRows_([a],config)[0];assert.equal(row[13],'');assert.equal(row[14],'');});
// Exercise the actual doPost and installation paths with an in-memory Sheets double.
let locked=0;
class Range {
 constructor(sheet,row,col,count,width){Object.assign(this,{sheet,row,col,count,width});assert(row+count-1<=sheet.maxRows);assert(col+width-1<=sheet.maxCols);}
 getValues(){return Array.from({length:this.count},(_,r)=>Array.from({length:this.width},(_,c)=>(this.sheet.data[this.row+r-1]||[])[this.col+c-1]??''));}
 getValue(){return this.getValues()[0][0];}
 setValues(values){assert.equal(values.length,this.count);values.forEach((values,r)=>{assert.equal(values.length,this.width);for(let c=0;c<this.width;c++){if(!this.sheet.data[this.row+r-1])this.sheet.data[this.row+r-1]=[];this.sheet.data[this.row+r-1][this.col+c-1]=values[c];}});return this;}
 clearContent(){this.setValues(Array.from({length:this.count},()=>Array(this.width).fill('')));return this;}
 setBackground(){return this;}setFontColor(){return this;}setFontWeight(){return this;}setWrap(){return this;}setNumberFormat(){return this;}
}
class Sheet {
 constructor(name){this.name=name;this.data=[];this.maxRows=1000;this.maxCols=26;}
 getLastRow(){let n=this.data.length;while(n&&!(this.data[n-1]||[]).some(v=>v!==''&&v!=null))n--;return n;}
 getMaxRows(){return this.maxRows;}getMaxColumns(){return this.maxCols;}
 insertRowsAfter(_,n){this.maxRows+=n;}insertColumnsAfter(_,n){this.maxCols+=n;}
 getRange(r,c,n=1,w=1){return new Range(this,r,c,n,w);}
 getDataRange(){return this.getRange(1,1,Math.max(1,this.getLastRow()),Math.max(1,...this.data.map(r=>r.length)));}
 setFrozenRows(){}setFrozenColumns(){}setHiddenGridlines(){}setColumnWidths(){}setColumnWidth(){}setRowHeight(){}
}
const tabs=new Map();const original=new Sheet('Existing monthly analysis');original.data=[['Original data',3000]];tabs.set(original.name,original);
const ss={getId:()=> 'test-spreadsheet-id',getSheetByName:name=>tabs.get(name)||null,insertSheet:name=>{const s=new Sheet(name);tabs.set(name,s);return s;}};
const properties=new Map();
context.SpreadsheetApp={getActiveSpreadsheet:()=>ss,openById:id=>{assert.equal(id,ss.getId());return ss;},flush:()=>{}};
context.PropertiesService={getScriptProperties:()=>({getProperty:k=>properties.get(k)||null,setProperty:(k,v)=>{properties.set(k,v);}})};
context.LockService={getScriptLock:()=>({waitLock:()=>{locked++;},releaseLock:()=>{locked--;}})};
context.Utilities.getUuid=()=>crypto.randomUUID();context.showCollectorConnection=()=>{};
context.ContentService={MimeType:{JSON:'application/json'},createTextOutput:body=>({body,setMimeType(){return this;}})};
test('Installer creates only its own tabs and expands wide report',()=>{context.installCollector();assert(tabs.get('FC Comparison').maxCols>=11+2*catalog.products.length);assert.deepEqual(original.data,[['Original data',3000]]);assert.equal(locked,0);assert.equal(properties.get('FC_SPREADSHEET_ID'),ss.getId());assert.equal(tabs.get('FC Routes').getLastRow(),1);});
tabs.get('FC Routes').getRange(2,1,1,5).setValues([['R01','Test pickup','Test destination',true,'Synthetic test route']]);
const pairing=properties.get('FC_PAIRING_TOKEN');
function post(records,token=pairing){return JSON.parse(context.doPost({postData:{contents:JSON.stringify({action:'submit',token,device_id:'device1111-2222-3333-4444-555555555555',records})}}).body);}
test('POST appends reviewed record',()=>{const result=post([quote]);assert.equal(result.ok,true);assert.equal(result.appended,1);assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('Retry acknowledges without duplicate',()=>{const result=post([quote]);assert.equal(result.ok,true);assert.equal(result.appended,0);assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('Conflicting ID rejects whole batch before append',()=>{const fresh={...quote,id:'fresh111-2222-3333-4444-555555555555'};const result=post([fresh,{...quote,low:3500,high:3500,raw:'3500'}]);assert.equal(result.ok,false);assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('POST with bad token cannot mutate data',()=>{assert.equal(post([quote],'wrong-token').ok,false);assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('Unreviewed batch cannot append',()=>assert.equal(post([{...quote,id:'fresh222-2222-3333-4444-555555555555',reviewed:false}]).ok,false));
test('Config requires authentication and returns only allowed providers',()=>{const reply=JSON.parse(context.doPost({postData:{contents:JSON.stringify({action:'config',token:pairing,device_id:'device1111-2222-3333-4444-555555555555'})}}).body);assert(reply.ok);assert(reply.config.products.every(p=>catalog.providers.some(c=>c.name===p.provider)));assert(!JSON.stringify(reply).includes(pairing));});
test('Installer rerun preserves quotes and pairing',()=>{context.installCollector();assert.equal(tabs.get('FC Quotes').getLastRow(),2);assert.equal(properties.get('FC_PAIRING_TOKEN'),pairing);assert.deepEqual(original.data,[['Original data',3000]]);});
test('Standalone installer uses the same exact target',()=>{context.SpreadsheetApp.getActiveSpreadsheet=()=>null;context.installCollector();assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('Different bound spreadsheet rejected',()=>{context.SpreadsheetApp.getActiveSpreadsheet=()=>({getId:()=> 'another-spreadsheet'});assert.throws(()=>context.installCollector());assert.equal(tabs.get('FC Quotes').getLastRow(),2);});
test('Unconfigured standalone cannot choose an implicit workbook',()=>{context.SpreadsheetApp.getActiveSpreadsheet=()=>null;properties.delete('FC_SPREADSHEET_ID');assert.throws(()=>context.installCollector());assert.equal(locked,0);assert.deepEqual(original.data,[['Original data',3000]]);});
const fullConfig=context.readCollectorConfig_(ss);
for(const provider of catalog.providers)test('Accept configured quote from '+provider.name,()=>{const product=fullConfig.products.find(p=>p.provider===provider.name);const row=context.validateCollectorRecord_({...quote,provider:provider.name,mode:product.mode,product_id:product.id},fullConfig,'device1111-2222-3333-4444-555555555555',now);assert.equal(row[6],provider.name);});
console.log(`PASS: ${checks} receiver validation, authentication, ingestion and comparison checks`);
