<#import "/templates/system/common/cstudio-support.ftl" as studio />
<div <@studio.componentAttr path=model.storeUrl ice=false /> >${model.content_html!''}</div>