export class Plan {
	public idPlan: number;

	public nombrePlan?: string | null;

	public precioPlan?: number | null;

	constructor(_idPlan: number, _nombrePlan?: string, _precioPlan?: number) {
		this.idPlan = _idPlan;
		this.nombrePlan = _nombrePlan;
		this.precioPlan = _precioPlan;
	}
}
