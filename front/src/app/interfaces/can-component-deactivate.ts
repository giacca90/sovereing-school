export interface CanComponentDeactivate {
	canDeactivate: () => boolean | Promise<boolean>;
}
