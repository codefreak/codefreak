import {injectable, inject} from "inversify";
import {
  FrontendApplication,
  FrontendApplicationContribution,
  OpenerService
} from "@theia/core/lib/browser";
import {WorkspaceService} from '@theia/workspace/lib/browser';
import {FileNavigatorContribution} from "@theia/navigator/lib/browser/navigator-contribution";
import {PreviewUri, PreviewHandler} from "@theia/preview/lib/browser"
import URI from "@theia/core/lib/common/uri"
import {FileStat} from "@theia/filesystem/lib/common";

// list of files that will opened initially
// the order is important if more than one file is found
const DEFAULT_FILES = [
  /^README\.md$/,
  /^(index|main)\.(md|js|ts|c|cpp|java|py|rb|php|htm|html)$/i
];

/**
 * Tries the default files in the defined order to find one matching file
 * @param files
 */
function findDefaultFile(files: FileStat[]): URI | undefined {
  for (let regex of DEFAULT_FILES) {
    for (let file of files) {
      const path = file.uri;
      const basename = path.substr(path.lastIndexOf('/') + 1);
      if (regex.test(basename)) {
        return new URI(file.uri);
      }
    }
  }
}

@injectable()
export class TheiaBrandingContribution implements FrontendApplicationContribution {

  constructor(
    @inject(FileNavigatorContribution) private readonly navigatorContribution: FileNavigatorContribution,
    @inject(WorkspaceService) private readonly workspaceService: WorkspaceService,
    @inject(OpenerService) private readonly openerService: OpenerService,
    @inject(PreviewHandler) private readonly previewHandler: PreviewHandler
  ) {
  }

  async initializeLayout(app: FrontendApplication): Promise<void> {
    await this.openNavigator();
    await this.openDefaultFile();
  }

  /**
   * Open the file navigator in the sidebar
   */
  async openNavigator(): Promise<void> {
    await this.navigatorContribution.openView({reveal: true});
  }

  /**
   * Try to open a README.md file or something else that looks like a good default
   */
  async openDefaultFile(): Promise<void> {
    const workspace = this.workspaceService.workspace;
    if (workspace && workspace.children) {
      const files = workspace.children;
      let uri = findDefaultFile(files);
      if (uri) {
        // show a nice preview if the preview handler can open the file
        if (this.previewHandler.canHandle(uri)) {
          uri = PreviewUri.encode(uri)
        }
        const opener = await this.openerService.getOpener(uri);
        opener.open(uri)
      }
    }
  }
}
