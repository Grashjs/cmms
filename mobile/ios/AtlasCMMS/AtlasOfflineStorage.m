#import <React/RCTBridgeModule.h>
#import <Foundation/Foundation.h>

@interface AtlasOfflineStorage : NSObject <RCTBridgeModule>
@end

@implementation AtlasOfflineStorage
RCT_EXPORT_MODULE();
+ (BOOL)requiresMainQueueSetup { return NO; }

RCT_REMAP_METHOD(prepareDirectory,
                 prepareDirectoryWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
  NSFileManager *manager = NSFileManager.defaultManager;
  NSURL *documents = [manager URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask].firstObject;
  NSURL *directory = [documents URLByAppendingPathComponent:@"atlas-offline" isDirectory:YES];
  NSError *error = nil;
  NSDictionary *attributes = @{ NSFileProtectionKey: NSFileProtectionComplete };
  if (![manager createDirectoryAtURL:directory withIntermediateDirectories:YES attributes:attributes error:&error] ||
      ![manager setAttributes:attributes ofItemAtPath:directory.path error:&error] ||
      ![directory setResourceValue:@YES forKey:NSURLIsExcludedFromBackupKey error:&error]) {
    reject(@"offline.storage.protection", @"Unable to protect local storage", error);
    return;
  }
  // Apply the same protection to existing databases, WAL/SHM and authentication snapshots.
  NSDirectoryEnumerator *enumerator = [manager enumeratorAtURL:directory
      includingPropertiesForKeys:nil options:0 errorHandler:nil];
  for (NSURL *url in enumerator) {
    if (![manager setAttributes:attributes ofItemAtPath:url.path error:&error] ||
        ![url setResourceValue:@YES forKey:NSURLIsExcludedFromBackupKey error:&error]) {
      reject(@"offline.storage.protection", @"Unable to protect existing local data", error);
      return;
    }
  }
  NSString *uri = directory.absoluteString;
  resolve([uri hasSuffix:@"/"] ? uri : [uri stringByAppendingString:@"/"]);
}
@end
