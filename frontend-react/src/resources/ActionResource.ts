import AuthResource from './AuthResource';

export default class ActionResource extends AuthResource {
    readonly date: string = new Date().toISOString();
    readonly user: string = '';
    readonly action: string | undefined = undefined;

    pk() {
        return this.user + this.date;
    }
};