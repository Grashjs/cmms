#import <React/RCTBridgeModule.h>
#import <React/RCTUtils.h>
#import <Foundation/Foundation.h>
#import <QuickLook/QuickLook.h>

@interface AtlasOfflinePreviewItem : NSObject <QLPreviewItem>
@property (nonatomic, strong) NSURL *previewItemURL;
@property (nonatomic, copy) NSString *previewItemTitle;
@end
@implementation AtlasOfflinePreviewItem
@end

@interface AtlasOfflineStorage : NSObject <RCTBridgeModule, QLPreviewControllerDataSource, QLPreviewControllerDelegate>
@property (nonatomic, strong) QLPreviewController *previewController;
@property (nonatomic, strong) AtlasOfflinePreviewItem *previewItem;
@end

@implementation AtlasOfflineStorage
RCT_EXPORT_MODULE();
+ (BOOL)requiresMainQueueSetup { return NO; }

- (NSURL *)privateURL:(NSString *)value allowDirectory:(BOOL)allowDirectory
{
  NSURL *url = [NSURL URLWithString:value];
  if (!url.isFileURL) return nil;
  url = url.URLByResolvingSymlinksInPath.URLByStandardizingPath;
  NSURL *documents = [NSFileManager.defaultManager URLsForDirectory:NSDocumentDirectory inDomains:NSUserDomainMask].firstObject;
  NSString *root = [documents URLByAppendingPathComponent:@"atlas-offline" isDirectory:YES].URLByResolvingSymlinksInPath.path;
  BOOL directory = NO;
  if (![url.path hasPrefix:[root stringByAppendingString:@"/"]] ||
      ![NSFileManager.defaultManager fileExistsAtPath:url.path isDirectory:&directory] || (directory && !allowDirectory)) return nil;
  return url;
}

RCT_REMAP_METHOD(protectFile,
                 protectFile:(NSString *)value
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
  NSURL *url = [self privateURL:value allowDirectory:YES];
  NSError *error = nil;
  if (!url || ![NSFileManager.defaultManager setAttributes:@{ NSFileProtectionKey: NSFileProtectionComplete }
        ofItemAtPath:url.path error:&error] ||
      ![url setResourceValue:@YES forKey:NSURLIsExcludedFromBackupKey error:&error]) {
    reject(@"offline.storage.protection", @"Unable to protect the private file", error);
    return;
  }
  resolve(nil);
}

RCT_REMAP_METHOD(openDocument,
                 openDocument:(NSString *)value
                 name:(NSString *)name
                 mimeType:(NSString *)mimeType
                 resolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    NSURL *url = [self privateURL:value allowDirectory:NO];
    AtlasOfflinePreviewItem *item = [AtlasOfflinePreviewItem new];
    item.previewItemURL = url;
    item.previewItemTitle = name;
    UIViewController *presenter = RCTPresentedViewController();
    if (!url || !presenter || self.previewController || ![QLPreviewController canPreviewItem:item]) {
      reject(@"offline.file.open", @"Unable to preview the private document", nil);
      return;
    }
    self.previewItem = item;
    QLPreviewController *controller = [QLPreviewController new];
    controller.dataSource = self;
    controller.delegate = self;
    self.previewController = controller;
    [presenter presentViewController:controller animated:YES completion:^{ resolve(nil); }];
  });
}

RCT_REMAP_METHOD(closeDocument,
                 closeDocumentWithResolver:(RCTPromiseResolveBlock)resolve
                 rejecter:(RCTPromiseRejectBlock)reject)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    if (!self.previewController) { resolve(nil); return; }
    [self.previewController dismissViewControllerAnimated:NO completion:^{
      self.previewController = nil;
      self.previewItem = nil;
      resolve(nil);
    }];
  });
}

- (NSInteger)numberOfPreviewItemsInPreviewController:(QLPreviewController *)controller { return self.previewItem ? 1 : 0; }
- (id<QLPreviewItem>)previewController:(QLPreviewController *)controller previewItemAtIndex:(NSInteger)index { return self.previewItem; }
- (void)previewControllerDidDismiss:(QLPreviewController *)controller
{
  self.previewController = nil;
  self.previewItem = nil;
}

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
