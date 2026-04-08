export type ApiResponse<T> = {
  success: boolean;
  errorCode: string;
  message: string;
  data: T;
  localeMeta?: {
    requestedLocale: string;
    resolvedLocale: string;
    fallbackApplied: boolean;
  } | null;
};

export type MallEntryView = {
  entryType: string;
  storefrontId: string;
  terminalType: string;
  entryStatus: string;
  homePageId: string;
};

export type AdminWorkbenchSummary = {
  pendingReviewCount: number;
  slaWarningCount: number;
  module: string;
};
