package com.spoiledmilk.spritebaker;

import java.nio.file.Path;

/** Phase-6 automation entry point; independent of the desktop and compatibility CLIs. */
public final class HeadlessMain {
    private HeadlessMain(){}
    public static void main(String[] args){int status=run(args);if(status!=0)System.exit(status);}
    static int run(String[] args){try{BatchProcessor.Request request=parse(args);BatchProcessor.Result result=new BatchProcessor().process(request);System.out.print(BatchManifest.gson().toJson(result.report));System.out.println();System.err.println((result.accepted?"Report: ":"FAILED report: ")+result.reportPath);return result.accepted?0:1;}catch(Exception e){System.err.println("RSC Sprite Baker headless error: "+root(e));return 2;}}
    static BatchProcessor.Request parse(String[] args){if(args.length==0)usage();boolean single="single".equals(args[0]),batch="batch".equals(args[0]);if(!single&&!batch)usage();BatchProcessor.Request request=new BatchProcessor.Request();for(int i=1;i<args.length;i++){String option=args[i];switch(option){case"--validate-only":if(request.mode!=BatchProcessor.Mode.EXPORT)usage();request.mode=BatchProcessor.Mode.VALIDATE_ONLY;break;case"--dry-run":if(request.mode!=BatchProcessor.Mode.EXPORT)usage();request.mode=BatchProcessor.Mode.DRY_RUN;break;default:if(i+1>=args.length)usage();String value=args[++i];switch(option){case"--cache":request.cache=Path.of(value);break;case"--project":request.singleProject=Path.of(value);break;case"--batch-manifest":request.manifestFile=Path.of(value);break;case"--output-dir":request.output=Path.of(value);break;case"--name":request.singleOutputName=value;break;case"--report":request.reportFile=Path.of(value);break;default:usage();}}}
        if(request.cache==null||request.output==null)usage();if(single&&(request.singleProject==null||request.singleOutputName==null||request.manifestFile!=null))usage();if(batch&&(request.manifestFile==null||request.singleProject!=null||request.singleOutputName!=null))usage();return request;}
    private static String root(Throwable e){while(e.getCause()!=null)e=e.getCause();return e.getMessage()==null?e.toString():e.getMessage();}
    private static void usage(){throw new IllegalArgumentException("usage: HeadlessMain single --cache PATH --project FILE --output-dir DIR --name NAME [--validate-only|--dry-run] [--report FILE]\n   or: HeadlessMain batch --cache PATH --batch-manifest FILE --output-dir DIR [--validate-only|--dry-run] [--report FILE]");}
}
