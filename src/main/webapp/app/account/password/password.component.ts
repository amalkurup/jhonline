/**
 * Copyright 2017-2019 the original author or authors from the JHipster Online project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit } from '@angular/core';

import { Principal } from 'app/core';
import { PasswordService } from './password.service';

@Component({
    selector: 'jhi-password',
    templateUrl: './password.component.html'
})
export class PasswordComponent implements OnInit {
    doNotMatch: string;
    error: string;
    success: string;
    account: any;
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;

    constructor(private passwordService: PasswordService, private principal: Principal) {}

    ngOnInit() {
        this.principal.identity().then(account => {
            this.account = account;
        });
    }

    changePassword() {
        if (this.newPassword !== this.confirmPassword) {
            this.error = null;
            this.success = null;
            this.doNotMatch = 'ERROR';
        } else {
            this.doNotMatch = null;
            this.passwordService.save(this.newPassword, this.currentPassword).subscribe(
                () => {
                    this.error = null;
                    this.success = 'OK';
                },
                () => {
                    this.success = null;
                    this.error = 'ERROR';
                }
            );
        }
    }
}
