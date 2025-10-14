import { bootstrapApplication, BootstrapContext } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { config } from './app/app.config.server';
import { InitService } from './app/services/init.service';

export default function (context: BootstrapContext) {
	return bootstrapApplication(AppComponent, config, context).then(async (appRef) => {
		const initService = appRef.injector.get(InitService);
		initService.preloadFromGlobalCache();
		await initService.carga();
		return appRef;
	});
}
