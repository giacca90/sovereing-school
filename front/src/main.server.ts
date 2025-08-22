import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { config } from './app/app.config.server';
import { InitService } from './app/services/init.service';
import { setInitService } from './init-singleton';

export default function () {
	return bootstrapApplication(AppComponent, config).then(async (appRef) => {
		const initService = appRef.injector.get(InitService);
		setInitService(initService);
		await initService.carga();
		return appRef;
	});
}
