import { TheiaBrandingContribution } from './theia-branding-contribution';
import { ContainerModule } from "inversify";
import {FrontendApplicationContribution} from "@theia/core/lib/browser";

export default new ContainerModule(bind => {
  bind(FrontendApplicationContribution).to(TheiaBrandingContribution).inSingletonScope();
});
